package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final Pattern KS_PATTERN = Pattern.compile("\\bks\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final CoSoLuuTruRepository listingRepo;
    private final AiClient aiClient;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Autowired
    public ChatbotService(CoSoLuuTruRepository listingRepo, AiClient aiClient) {
        this.listingRepo = listingRepo;
        this.aiClient = (aiClient != null) ? aiClient : (userMessage, suggested) -> Optional.empty();
    }

    public ChatbotService(CoSoLuuTruRepository listingRepo, ChatbotGeminiClient geminiClient) {
        this(listingRepo, (AiClient) geminiClient);
    }

    public ChatMessageResponse handleMessage(ChatMessageRequest request) {
        String message = request.message() != null ? request.message().trim() : "";
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId().trim()
                : UUID.randomUUID().toString();

        // 1. Trich xuat nhu cau nguoi dung tu tin nhan (Nlp Intent Extraction)
        ParsedIntent intent = parseUserIntent(message);

        // 1.1. Ngoai pham vi du an (ngoai tinh, chu de khong lien quan)
        if (intent.isOutOfScope) {
            return new ChatMessageResponse(
                    "HotelNT hiện tại chỉ hỗ trợ tư vấn và tìm kiếm cơ sở lưu trú tại thành phố Nha Trang. "
                            + "Nếu bạn có kế hoạch du lịch Nha Trang, mình rất sẵn lòng hỗ trợ!",
                    sessionId,
                    List.of());
        }

        // 1.2. So sanh ung dung (Agoda, Booking, Traveloka...)
        if (intent.isApplicationComparison) {
            return new ChatMessageResponse(
                    "HotelNT là nền tảng giúp bạn tìm và chọn cơ sở lưu trú tại Nha Trang. "
                            + "Bạn có thể cho mình biết khu vực, loại hình hoặc mức giá mong muốn để mình tìm trong dữ liệu HotelNT.",
                    sessionId,
                    List.of());
        }

        // 1.3. Xu ly danh sach goi y va phan hoi fallback
        List<SuggestedListing> suggested;
        String fallbackReply;

        if (!intent.hasSearchCriteria()) {
            // Khong co tieu chi tim kiem (chao hoi, hoi chung chung):
            // suggestedListings rong de khong tra bua bai, de AI tu do tro chuyen tu nhien
            suggested = List.of();
            fallbackReply = "Chào bạn! Bạn vui lòng cho mình biết thêm thông tin về loại hình lưu trú (khách sạn, homestay, villa...), "
                    + "mức ngân sách hoặc khu vực mong muốn tại Nha Trang để mình hỗ trợ tìm kiếm tốt nhất nhé!";
        } else {
            // 2. Lay toan bo CSLT da duyet (APPROVED) va chu co so hoat dong (ACTIVE)
            List<CoSoLuuTru> visibleListings = listingRepo.findAll(ChatbotSpecs.visibleApproved());

            // 3. Loc cung va cham diem CSLT phu hop
            suggested = rankAndMapListings(visibleListings, intent);

            if (suggested.isEmpty()) {
                fallbackReply = "Hiện tại chưa có cơ sở phù hợp với yêu cầu của bạn. Bạn vui lòng thử điều chỉnh lại mức giá hoặc khu vực nhé!";
            } else {
                fallbackReply = createFallbackReply(suggested);
            }
        }

        // 4. Goi AI Client tao cau tra loi tu nhien (cho moi cau hoi thuong ngay), kem fallback tu dong
        Optional<String> aiReply = aiClient.generateReply(message, suggested);
        String finalReplyText = aiReply.filter(s -> !s.isBlank()).orElse(fallbackReply);

        return new ChatMessageResponse(finalReplyText, sessionId, suggested);
    }

    private List<SuggestedListing> rankAndMapListings(List<CoSoLuuTru> listings, ParsedIntent intent) {
        if (listings == null || listings.isEmpty() || intent.isOutOfScope || !intent.hasSearchCriteria()) {
            return List.of();
        }

        List<ScoredListing> scored = new ArrayList<>();
        for (CoSoLuuTru c : listings) {
            // Loc cung truoc khi cham diem
            if (!passesHardFilters(c, intent)) {
                continue;
            }

            int score = calculateScore(c, intent);
            if (score > 0) {
                scored.add(new ScoredListing(c, score));
            }
        }

        // Sap xep theo diem phu hop giam dan, neu bang diem thi gia thap xep truoc (null xep cuoi)
        scored.sort((a, b) -> {
            int cmp = Integer.compare(b.score, a.score);
            if (cmp != 0) {
                return cmp;
            }
            BigDecimal g1 = a.listing.getGia();
            BigDecimal g2 = b.listing.getGia();
            if (g1 == null && g2 == null) return 0;
            if (g1 == null) return 1;
            if (g2 == null) return -1;
            return g1.compareTo(g2);
        });

        // Gioi han toi da 5 goi y phu hop nhat (khong tu noi dieu kien, khong bu cho du 5)
        return scored.stream()
                .limit(5)
                .map(s -> toSuggestedListing(s.listing))
                .toList();
    }

    /**
     * Loc cung truoc khi cham diem:
     * - Co maxPrice thi loai gia == null hoac gia > maxPrice
     * - Co minPrice thi loai gia == null hoac gia < minPrice
     * - Co loai hinh thi loai listing khac loai
     * - Co vi tri thi chi giu listing khop vi tri
     * - Co tien ich thi chi giu listing khop tien ich
     */
    private boolean passesHardFilters(CoSoLuuTru c, ParsedIntent intent) {
        // Kiem tra gia toi da
        if (intent.maxPrice != null) {
            if (c.getGia() == null || c.getGia().compareTo(BigDecimal.valueOf(intent.maxPrice)) > 0) {
                return false;
            }
        }

        // Kiem tra gia toi thieu
        if (intent.minPrice != null) {
            if (c.getGia() == null || c.getGia().compareTo(BigDecimal.valueOf(intent.minPrice)) < 0) {
                return false;
            }
        }

        // Kiem tra loai hinh
        if (intent.propertyTypeKeyword != null) {
            if (!matchesPropertyType(c, intent.propertyTypeKeyword)) {
                return false;
            }
        }

        // Kiem tra vi tri
        if (!intent.locationKeywords.isEmpty()) {
            if (!matchesAnyLocation(c, intent.locationKeywords)) {
                return false;
            }
        }

        // Kiem tra tien ich
        if (!intent.amenityKeywords.isEmpty()) {
            if (!matchesAnyAmenity(c, intent.amenityKeywords)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Cham diem: Bo base score. Chi tinh diem cac tieu chi thuc su khop.
     */
    private int calculateScore(CoSoLuuTru c, ParsedIntent intent) {
        int score = 0;

        // 1. Loai hinh
        if (intent.propertyTypeKeyword != null && matchesPropertyType(c, intent.propertyTypeKeyword)) {
            score += 10;
        }

        // 2. Gia
        if (intent.maxPrice != null || intent.minPrice != null) {
            score += 8;
        }

        // 3. Vi tri
        String wardName = (c.getPhuong() != null && c.getPhuong().getTenPhuong() != null)
                ? c.getPhuong().getTenPhuong().toLowerCase() : "";
        String address = (c.getDiaChiChiTiet() != null) ? c.getDiaChiChiTiet().toLowerCase() : "";
        String name = (c.getTenCSLT() != null) ? c.getTenCSLT().toLowerCase() : "";
        String desc = (c.getMoTa() != null) ? c.getMoTa().toLowerCase() : "";

        for (String loc : intent.locationKeywords) {
            if (wardName.contains(loc) || address.contains(loc) || name.contains(loc) || desc.contains(loc)) {
                score += 6;
            }
        }

        // 4. Tien ich
        for (String am : intent.amenityKeywords) {
            boolean matched = name.contains(am) || desc.contains(am);
            if (!matched && c.getTienIch() != null) {
                matched = c.getTienIch().stream()
                        .filter(t -> t.getTenTienIch() != null)
                        .anyMatch(t -> t.getTenTienIch().toLowerCase().contains(am));
            }
            if (matched) {
                score += 4;
            }
        }

        return score;
    }

    private boolean matchesPropertyType(CoSoLuuTru c, String keyword) {
        if (keyword == null) return true;
        String typeName = (c.getLoai() != null && c.getLoai().getTenLoai() != null)
                ? c.getLoai().getTenLoai().toLowerCase()
                : "";
        String name = (c.getTenCSLT() != null) ? c.getTenCSLT().toLowerCase() : "";

        if (typeName.contains(keyword) || name.contains(keyword)) {
            return true;
        }
        if ("khách sạn".equals(keyword)) {
            return KS_PATTERN.matcher(name).find() || name.contains("hotel") || typeName.contains("hotel");
        }
        return false;
    }

    private boolean matchesAnyLocation(CoSoLuuTru c, List<String> locationKeywords) {
        String wardName = (c.getPhuong() != null && c.getPhuong().getTenPhuong() != null)
                ? c.getPhuong().getTenPhuong().toLowerCase() : "";
        String address = (c.getDiaChiChiTiet() != null) ? c.getDiaChiChiTiet().toLowerCase() : "";
        String name = (c.getTenCSLT() != null) ? c.getTenCSLT().toLowerCase() : "";
        String desc = (c.getMoTa() != null) ? c.getMoTa().toLowerCase() : "";

        for (String loc : locationKeywords) {
            if (wardName.contains(loc) || address.contains(loc) || name.contains(loc) || desc.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnyAmenity(CoSoLuuTru c, List<String> amenityKeywords) {
        String name = (c.getTenCSLT() != null) ? c.getTenCSLT().toLowerCase() : "";
        String desc = (c.getMoTa() != null) ? c.getMoTa().toLowerCase() : "";

        for (String am : amenityKeywords) {
            if (name.contains(am) || desc.contains(am)) {
                return true;
            }
            if (c.getTienIch() != null) {
                boolean inAmenities = c.getTienIch().stream()
                        .filter(t -> t.getTenTienIch() != null)
                        .anyMatch(t -> t.getTenTienIch().toLowerCase().contains(am));
                if (inAmenities) {
                    return true;
                }
            }
        }
        return false;
    }

    private SuggestedListing toSuggestedListing(CoSoLuuTru c) {
        String mainPhoto = null;
        if (c.getMedia() != null) {
            mainPhoto = c.getMedia().stream()
                    .filter(m -> "IMAGE".equalsIgnoreCase(m.getLoai()))
                    .map(Media::getDuongDan)
                    .findFirst()
                    .orElse(null);
        }

        String phone = null;
        String zalo = null;
        String fb = null;
        if (c.getLienHe() != null) {
            for (LienHe lh : c.getLienHe()) {
                if (lh.getLoaiLienHe() == null)
                    continue;
                switch (lh.getLoaiLienHe().toUpperCase()) {
                    case "SO_DIEN_THOAI" -> phone = lh.getGiaTri();
                    case "ZALO" -> zalo = lh.getGiaTri();
                    case "FACEBOOK" -> fb = lh.getGiaTri();
                }
            }
        }

        return new SuggestedListing(
                c.getMaCSLT(),
                c.getTenCSLT(),
                c.getLoai() != null ? c.getLoai().getTenLoai() : null,
                c.getGia() != null ? c.getGia().longValue() : null,
                Times.toInstant(c.getNgayCapNhatGia() != null ? c.getNgayCapNhatGia() : c.getNgayTao()),
                UrlHelper.toPublicUrl(publicBaseUrl, mainPhoto),
                new ChatContactInfo(phone, zalo, fb));
    }

    private String createFallbackReply(List<SuggestedListing> suggested) {
        if (suggested == null || suggested.isEmpty()) {
            return "Hiện tại chưa có cơ sở phù hợp với yêu cầu của bạn. Bạn vui lòng thử điều chỉnh lại mức giá hoặc khu vực nhé!";
        }
        return "Mình tìm được " + suggested.size() + " cơ sở phù hợp với yêu cầu của bạn:";
    }

    private ParsedIntent parseUserIntent(String message) {
        String lower = message.toLowerCase();
        ParsedIntent intent = new ParsedIntent();

        // 1. Nhan dien loai co so (dung regex \\bks\\b de tranh khop nham "ks")
        if (lower.contains("villa") || lower.contains("biệt thự")) {
            intent.propertyTypeKeyword = "villa";
        } else if (lower.contains("resort") || lower.contains("nghỉ dưỡng")) {
            intent.propertyTypeKeyword = "resort";
        } else if (lower.contains("homestay")) {
            intent.propertyTypeKeyword = "homestay";
        } else if (lower.contains("khách sạn") || lower.contains("hotel") || KS_PATTERN.matcher(lower).find()) {
            intent.propertyTypeKeyword = "khách sạn";
        } else if (lower.contains("căn hộ") || lower.contains("apartment")) {
            intent.propertyTypeKeyword = "căn hộ";
        }

        // 2. Nhan dien muc gia (khoang gia, gia tran maxPrice, gia san minPrice)
        parsePrices(lower, intent);

        // 3. Nhan dien dia diem / view
        List<String> locTerms = List.of(
                "view biển", "sát biển", "gần biển", "biển", "trần phú",
                "lộc thọ", "phước hải", "vĩnh phước", "trung tâm", "chợ đêm",
                "bãi dài", "vĩnh nguyên", "vĩnh trường", "vĩnh hải", "vĩnh hòa",
                "vạn thạnh", "phương sài", "hòn chồng"
        );
        for (String term : locTerms) {
            if (lower.contains(term)) {
                intent.locationKeywords.add(term);
            }
        }

        // 4. Nhan dien tien ich
        List<String> amTerms = List.of("hồ bơi", "bể bơi", "wifi", "bếp", "bãi đỗ xe", "đậu xe", "điều hòa", "máy lạnh", "ăn sáng", "buffet");
        for (String term : amTerms) {
            if (lower.contains(term)) {
                intent.amenityKeywords.add(term);
            }
        }

        // 5. Nhan dien yeu cau ngoai pham vi Nha Trang
        List<String> outOfScopeKeywords = List.of(
                "đà lạt", "da lat", "hà nội", "ha noi", "sài gòn", "sai gon",
                "hồ chí minh", "ho chi minh", "đà nẵng", "da nang", "vũng tàu", "vung tau", "phú quốc", "phu quoc",
                "sa pa", "sapa"
        );
        for (String kw : outOfScopeKeywords) {
            if (lower.contains(kw)) {
                intent.isOutOfScope = true;
                break;
            }
        }

        intent.isApplicationComparison = lower.contains("agoda")
                || lower.contains("booking.com")
                || lower.contains("traveloka")
                || (lower.contains("ứng dụng")
                        && (lower.contains("tốt nhất") || lower.contains("chọn khách sạn")
                                || lower.contains("app nào") || lower.contains("nền tảng nào")));

        return intent;
    }

    private void parsePrices(String lower, ParsedIntent intent) {
        // Range: tu X den Y / X - Y
        Pattern pRange = Pattern.compile("(?:từ\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(k|nghìn|ngàn|triệu|tr|trieu)?\\s*(?:-|đến|tới)\\s*(\\d+(?:[.,]\\d+)?)\\s*(k|nghìn|ngàn|triệu|tr|trieu|đ|đồng|vnd)?");
        Matcher mRange = pRange.matcher(lower);
        if (mRange.find()) {
            String u2 = mRange.group(4);
            String u1 = mRange.group(2) != null ? mRange.group(2) : u2;
            Long min = parsePriceAmount(mRange.group(1), u1);
            Long max = parsePriceAmount(mRange.group(3), u2);
            if (min != null) intent.minPrice = min;
            if (max != null) intent.maxPrice = max;
            return;
        }

        // Min: tren / > / toi thieu
        Pattern pMin = Pattern.compile("(?:trên|>|tối thiểu|từ)\\s*(\\d+(?:[.,]\\d+)?)\\s*(k|nghìn|ngàn|triệu|tr|trieu|đ|đồng|vnd)?");
        Matcher mMin = pMin.matcher(lower);
        if (mMin.find()) {
            Long min = parsePriceAmount(mMin.group(1), mMin.group(2));
            if (min != null) {
                intent.minPrice = min;
                return;
            }
        }

        // Max: duoi / < / toi da / den / tam / khoang / gia
        Pattern pTrieu = Pattern.compile("(?:dưới|<|tối đa|đến|tầm|khoảng|giá)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr|trieu)");
        Matcher mTrieu = pTrieu.matcher(lower);
        if (mTrieu.find()) {
            try {
                double val = Double.parseDouble(mTrieu.group(1).replace(",", "."));
                intent.maxPrice = (long) (val * 1_000_000);
                return;
            } catch (Exception ignored) {
            }
        }

        Pattern pK = Pattern.compile("(?:dưới|<|tối đa|đến|tầm|khoảng|giá)?\\s*(\\d+)\\s*(?:k|nghìn|ngàn)");
        Matcher mK = pK.matcher(lower);
        if (mK.find()) {
            try {
                long val = Long.parseLong(mK.group(1));
                intent.maxPrice = val * 1_000;
                return;
            } catch (Exception ignored) {
            }
        }

        Pattern pDong = Pattern.compile("(?:dưới|<|tối đa|đến|tầm|khoảng|giá)?\\s*(\\d{1,3}(?:[.,]\\d{3})+)\\s*(?:đ|đồng|vnd)?");
        Matcher mDong = pDong.matcher(lower);
        if (mDong.find()) {
            try {
                intent.maxPrice = Long.parseLong(mDong.group(1).replace(".", "").replace(",", ""));
            } catch (Exception ignored) {
            }
        }
    }

    private Long parsePriceAmount(String numStr, String unitStr) {
        if (numStr == null || numStr.isBlank()) return null;
        try {
            double val = Double.parseDouble(numStr.replace(",", "."));
            if (unitStr != null) {
                String u = unitStr.toLowerCase();
                if (u.contains("tr") || u.contains("triệu") || u.contains("trieu")) {
                    return (long) (val * 1_000_000);
                }
                if (u.contains("k") || u.contains("nghìn") || u.contains("ngàn")) {
                    return (long) (val * 1_000);
                }
            }
            if (val >= 10_000) {
                return (long) val;
            }
            return (long) val;
        } catch (Exception e) {
            return null;
        }
    }

    private static class ParsedIntent {
        boolean isOutOfScope;
        boolean isApplicationComparison;
        String propertyTypeKeyword;
        Long maxPrice;
        Long minPrice;
        List<String> locationKeywords = new ArrayList<>();
        List<String> amenityKeywords = new ArrayList<>();

        boolean hasSearchCriteria() {
            return propertyTypeKeyword != null || maxPrice != null || minPrice != null
                    || !locationKeywords.isEmpty() || !amenityKeywords.isEmpty();
        }
    }

    private record ScoredListing(CoSoLuuTru listing, int score) {
    }
}
