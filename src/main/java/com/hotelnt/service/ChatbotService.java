package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatbotService {

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

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String s = input.toLowerCase().replace("đ", "d").replace("Đ", "d");
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(s).replaceAll("");
        return s.trim();
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

        // Kiem tra loai hinh: chi so khop voi loai.tenLoai (khong so voi ten co so)
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
     * Cham diem: Bo base score.
     * Moi nhom tieu chi (loai, gia, vi tri, tien ich) chi cong diem mot lan cho moi co so;
     * khong cong don cac tu khoa trung nghia.
     */
    private int calculateScore(CoSoLuuTru c, ParsedIntent intent) {
        int score = 0;

        // 1. Nhom Loai hinh (+10, toi da 1 lan)
        if (intent.propertyTypeKeyword != null && matchesPropertyType(c, intent.propertyTypeKeyword)) {
            score += 10;
        }

        // 2. Nhom Gia (+8, toi da 1 lan)
        if (intent.maxPrice != null || intent.minPrice != null) {
            score += 8;
        }

        // 3. Nhom Vi tri (+6, toi da 1 lan bat ke bao nhieu tu khoa vi tri khop)
        if (!intent.locationKeywords.isEmpty() && matchesAnyLocation(c, intent.locationKeywords)) {
            score += 6;
        }

        // 4. Nhom Tien ich (+4, toi da 1 lan bat ke bao nhieu tien ich khop)
        if (!intent.amenityKeywords.isEmpty() && matchesAnyAmenity(c, intent.amenityKeywords)) {
            score += 4;
        }

        return score;
    }

    /**
     * Chi so khop voi loai.tenLoai (giu ngoai le "khach san" khop loai co "hotel").
     * Khong so voi ten co so.
     */
    private boolean matchesPropertyType(CoSoLuuTru c, String keyword) {
        if (keyword == null) return true;
        if (c.getLoai() == null || c.getLoai().getTenLoai() == null) {
            return false;
        }
        String normTypeName = normalize(c.getLoai().getTenLoai());
        String normKeyword = normalize(keyword);

        if (normTypeName.contains(normKeyword)) {
            return true;
        }
        if ("khach san".equals(normKeyword)) {
            return normTypeName.contains("hotel");
        }
        return false;
    }

    private boolean isSeaKeyword(String normLoc) {
        return "bien".equals(normLoc) || "view bien".equals(normLoc)
                || "sat bien".equals(normLoc) || "gan bien".equals(normLoc);
    }

    /**
     * Chi so khop phuong va dia chi chi tiet.
     * Voi "bien", "view bien", "sat bien", "gan bien" thi so voi mo ta va tien ich, khong so voi ten co so.
     */
    private boolean matchesLocation(CoSoLuuTru c, String normLoc) {
        if (isSeaKeyword(normLoc)) {
            String normDesc = (c.getMoTa() != null) ? normalize(c.getMoTa()) : "";
            if (normDesc.contains(normLoc)) {
                return true;
            }
            if (c.getTienIch() != null) {
                for (TienIch t : c.getTienIch()) {
                    if (t.getTenTienIch() != null && normalize(t.getTenTienIch()).contains(normLoc)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            String normWard = (c.getPhuong() != null && c.getPhuong().getTenPhuong() != null)
                    ? normalize(c.getPhuong().getTenPhuong()) : "";
            String normAddress = (c.getDiaChiChiTiet() != null) ? normalize(c.getDiaChiChiTiet()) : "";
            return normWard.contains(normLoc) || normAddress.contains(normLoc);
        }
    }

    private boolean matchesAnyLocation(CoSoLuuTru c, List<String> locationKeywords) {
        for (String loc : locationKeywords) {
            if (matchesLocation(c, loc)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAmenity(CoSoLuuTru c, String normAm) {
        String normDesc = (c.getMoTa() != null) ? normalize(c.getMoTa()) : "";
        if (normDesc.contains(normAm)) {
            return true;
        }
        if (c.getTienIch() != null) {
            for (TienIch t : c.getTienIch()) {
                if (t.getTenTienIch() != null && normalize(t.getTenTienIch()).contains(normAm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAnyAmenity(CoSoLuuTru c, List<String> amenityKeywords) {
        for (String am : amenityKeywords) {
            if (matchesAmenity(c, am)) {
                return true;
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
        String norm = normalize(message);
        ParsedIntent intent = new ParsedIntent();

        // 1. Nhan dien loai co so (dung regex \\bks\\b de tranh khop nham "ks")
        if (norm.contains("villa") || norm.contains("biet thu")) {
            intent.propertyTypeKeyword = "villa";
        } else if (norm.contains("resort") || norm.contains("nghi duong")) {
            intent.propertyTypeKeyword = "resort";
        } else if (norm.contains("homestay")) {
            intent.propertyTypeKeyword = "homestay";
        } else if (norm.contains("khach san") || norm.contains("hotel") || KS_PATTERN.matcher(norm).find()) {
            intent.propertyTypeKeyword = "khach san";
        } else if (norm.contains("can ho") || norm.contains("apartment")) {
            intent.propertyTypeKeyword = "can ho";
        }

        // 2. Nhan dien muc gia
        parsePrices(norm, intent);

        // 3. Nhan dien dia diem / view (danh sach tu khoa da chuan hoa)
        List<String> locTerms = List.of(
                "view bien", "sat bien", "gan bien", "bien", "tran phu",
                "loc tho", "phuoc hai", "vinh phuoc", "trung tam", "cho dem",
                "bai dai", "vinh nguyen", "vinh truong", "vinh hai", "vinh hoa",
                "van thanh", "phuong sai", "hon chong"
        );
        for (String term : locTerms) {
            if (norm.contains(term)) {
                intent.locationKeywords.add(term);
            }
        }

        // 4. Nhan dien tien ich (danh sach tu khoa da chuan hoa)
        List<String> amTerms = List.of(
                "ho boi", "be boi", "wifi", "bep", "bai do xe", "dau xe", "dieu hoa", "may lanh", "an sang", "buffet"
        );
        for (String term : amTerms) {
            if (norm.contains(term)) {
                intent.amenityKeywords.add(term);
            }
        }

        // 5. Nhan dien yeu cau ngoai pham vi Nha Trang (khop theo ranh gioi tu \\b)
        List<String> outOfScopeKeywords = List.of(
                "da lat", "ha noi", "sai gon", "ho chi minh", "da nang",
                "vung tau", "phu quoc", "sa pa", "sapa"
        );
        for (String kw : outOfScopeKeywords) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(norm).find()) {
                intent.isOutOfScope = true;
                break;
            }
        }

        intent.isApplicationComparison = norm.contains("agoda")
                || norm.contains("booking.com")
                || norm.contains("traveloka")
                || (norm.contains("ung dung")
                        && (norm.contains("tot nhat") || norm.contains("chon khach san")
                                || norm.contains("app nao") || norm.contains("nen tang nao")));

        return intent;
    }

    private void parsePrices(String norm, ParsedIntent intent) {
        // Range: tu X den Y / X - Y
        Pattern pRange = Pattern.compile("(?:tu\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|k|nghin|ngan)?\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|k|nghin|ngan|d|dong|vnd)?\\b");
        Matcher mRange = pRange.matcher(norm);
        if (mRange.find()) {
            String u2 = mRange.group(4);
            String u1 = mRange.group(2) != null ? mRange.group(2) : u2;
            Long min = parsePriceAmount(mRange.group(1), u1);
            Long max = parsePriceAmount(mRange.group(3), u2);
            if (min != null || max != null) {
                if (min != null) intent.minPrice = min;
                if (max != null) intent.maxPrice = max;
                return;
            }
        }

        // Trieu / Tr: (tren | > | toi thieu | tu | duoi | < | toi da | den | tam | khoang | gia)? X trieu
        Pattern pTrieu = Pattern.compile("(tren|>|toi thieu|tu|duoi|<|toi da|den|tam|khoang|gia)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr)\\b");
        Matcher mTrieu = pTrieu.matcher(norm);
        if (mTrieu.find()) {
            String prefix = mTrieu.group(1);
            Long val = parsePriceAmount(mTrieu.group(2), "trieu");
            if (val != null) {
                if (prefix != null && isMinPrefix(prefix)) {
                    intent.minPrice = val;
                } else {
                    intent.maxPrice = val;
                }
                return;
            }
        }

        // K / Nghin / Ngan: (prefix)? X k
        Pattern pK = Pattern.compile("(tren|>|toi thieu|tu|duoi|<|toi da|den|tam|khoang|gia)?\\s*(\\d+)\\s*(?:k|nghin|ngan)\\b");
        Matcher mK = pK.matcher(norm);
        if (mK.find()) {
            String prefix = mK.group(1);
            Long val = parsePriceAmount(mK.group(2), "k");
            if (val != null) {
                if (prefix != null && isMinPrefix(prefix)) {
                    intent.minPrice = val;
                } else {
                    intent.maxPrice = val;
                }
                return;
            }
        }

        // Dong / VND hoac so khong don vi: chi nhan so co it nhat 5 chu so (>= 10000) hoac co dinh dang cham/phay
        Pattern pDong = Pattern.compile("(tren|>|toi thieu|tu|duoi|<|toi da|den|tam|khoang|gia)?\\s*(\\d{1,3}(?:[.,]\\d{3})+|\\d{5,})\\s*(?:d|dong|vnd)?\\b");
        Matcher mDong = pDong.matcher(norm);
        if (mDong.find()) {
            String prefix = mDong.group(1);
            String rawNum = mDong.group(2).replace(".", "").replace(",", "");
            Long val = parsePriceAmount(rawNum, null);
            if (val != null) {
                if (prefix != null && isMinPrefix(prefix)) {
                    intent.minPrice = val;
                } else {
                    intent.maxPrice = val;
                }
            }
        }
    }

    private boolean isMinPrefix(String prefix) {
        String p = prefix.trim();
        return p.equals("tren") || p.equals(">") || p.equals("toi thieu") || p.equals("tu");
    }

    public static Long parsePriceAmount(String numStr, String unitStr) {
        if (numStr == null || numStr.isBlank()) return null;
        try {
            double val = Double.parseDouble(numStr.replace(",", "."));
            if (unitStr != null && !unitStr.isBlank()) {
                String u = normalize(unitStr);
                if (u.contains("tr") || u.contains("trieu")) {
                    return (long) (val * 1_000_000);
                }
                if (u.contains("k") || u.contains("nghin") || u.contains("ngan")) {
                    return (long) (val * 1_000);
                }
                if (u.contains("d") || u.contains("dong") || u.contains("vnd")) {
                    if (val >= 10_000) {
                        return (long) val;
                    }
                    return null;
                }
            }
            // So khong co don vi chi duoc nhan la gia khi >= 10000
            if (val >= 10_000) {
                return (long) val;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class ParsedIntent {
        public boolean isOutOfScope;
        public boolean isApplicationComparison;
        public String propertyTypeKeyword;
        public Long maxPrice;
        public Long minPrice;
        public List<String> locationKeywords = new ArrayList<>();
        public List<String> amenityKeywords = new ArrayList<>();

        public boolean hasSearchCriteria() {
            return propertyTypeKeyword != null || maxPrice != null || minPrice != null
                    || !locationKeywords.isEmpty() || !amenityKeywords.isEmpty();
        }
    }

    private record ScoredListing(CoSoLuuTru listing, int score) {
    }
}
