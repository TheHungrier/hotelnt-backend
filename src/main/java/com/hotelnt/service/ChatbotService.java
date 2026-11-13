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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final CoSoLuuTruRepository listingRepo;
    private final ChatbotAiClient aiClient;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public ChatbotService(CoSoLuuTruRepository listingRepo, ChatbotAiClient aiClient) {
        this.listingRepo = listingRepo;
        this.aiClient = aiClient;
    }

    public ChatMessageResponse handleMessage(ChatMessageRequest request) {
        String message = request.message() != null ? request.message().trim() : "";
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId().trim()
                : UUID.randomUUID().toString();

        // 1. Lay toan bo CSLT da duyet (APPROVED) va chu co so hoat dong (ACTIVE)
        List<CoSoLuuTru> visibleListings = listingRepo.findAll(ChatbotSpecs.visibleApproved());

        // 2. Trich xuat nhu cau nguoi dung tu tin nhan (Nlp Intent Extraction)
        ParsedIntent intent = parseUserIntent(message);

        if (intent.isApplicationComparison) {
            return new ChatMessageResponse(
                    "HotelNT là nền tảng giúp bạn tìm và chọn cơ sở lưu trú tại Nha Trang. "
                            + "Bạn có thể cho mình biết khu vực, loại hình hoặc mức giá mong muốn để mình tìm trong dữ liệu HotelNT.",
                    sessionId,
                    List.of());
        }

        // 3. Cham diem va xep hang CSLT phu hop
        List<SuggestedListing> suggested = rankAndMapListings(visibleListings, intent);

        // 4. Goi AI (OpenRouter) tao cau tra loi tu nhien (kem fallback tu dong)
        String aiReply = aiClient.generateReply(message, suggested);
        String finalReplyText = (aiReply != null && !aiReply.isBlank()) ? aiReply : createFallbackReply(suggested);

        return new ChatMessageResponse(finalReplyText, sessionId, suggested);
    }

    private List<SuggestedListing> rankAndMapListings(List<CoSoLuuTru> listings, ParsedIntent intent) {
        if (listings.isEmpty() || intent.isOutOfScope || !intent.hasSearchCriteria()) {
            return List.of();
        }

        List<ScoredListing> scored = new ArrayList<>();
        for (CoSoLuuTru c : listings) {
            int score = calculateScore(c, intent);
            if (score > 0) {
                scored.add(new ScoredListing(c, score));
            }
        }

        // Sap xep theo diem phu hop giam dan, neu bang diem thi gia thap xep truoc
        scored.sort((a, b) -> {
            int cmp = Integer.compare(b.score, a.score);
            if (cmp != 0)
                return cmp;
            BigDecimal g1 = a.listing.getGia() != null ? a.listing.getGia() : BigDecimal.ZERO;
            BigDecimal g2 = b.listing.getGia() != null ? b.listing.getGia() : BigDecimal.ZERO;
            return g1.compareTo(g2);
        });

        // Gioi han toi da 5 goi y phu hop nhat
        return scored.stream()
                .limit(5)
                .map(s -> toSuggestedListing(s.listing))
                .toList();
    }

    private int calculateScore(CoSoLuuTru c, ParsedIntent intent) {
        int score = 1;
        String name = c.getTenCSLT() != null ? c.getTenCSLT().toLowerCase() : "";
        String desc = c.getMoTa() != null ? c.getMoTa().toLowerCase() : "";
        String address = c.getDiaChiChiTiet() != null ? c.getDiaChiChiTiet().toLowerCase() : "";
        String typeName = c.getLoai() != null && c.getLoai().getTenLoai() != null
                ? c.getLoai().getTenLoai().toLowerCase()
                : "";
        String wardName = c.getPhuong() != null && c.getPhuong().getTenPhuong() != null
                ? c.getPhuong().getTenPhuong().toLowerCase()
                : "";

        // Kiem tra loai co so
        if (intent.propertyTypeKeyword != null) {
            if (typeName.contains(intent.propertyTypeKeyword) || name.contains(intent.propertyTypeKeyword)) {
                score += 10;
            } else {
                return 0; // Loai co so la dieu kien bat buoc khi khach da chi dinh
            }
        }

        // Kiem tra khoang gia
        if (intent.maxPrice != null) {
            if (c.getGia() == null || c.getGia().compareTo(BigDecimal.valueOf(intent.maxPrice)) > 0) {
                return 0; // Khong gioi thieu co so vuot ngan sach
            }
            score += 8;
        }

        // Kiem tra vi tri / phuong
        for (String loc : intent.locationKeywords) {
            if (wardName.contains(loc) || address.contains(loc) || name.contains(loc) || desc.contains(loc)) {
                score += 6;
            }
        }

        // Kiem tra tien ich dac thu
        for (String am : intent.amenityKeywords) {
            if (desc.contains(am) || name.contains(am)) {
                score += 4;
            }
        }

        return score;
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
                toPublicImageUrl(mainPhoto),
                new ChatContactInfo(phone, zalo, fb));
    }

    private String toPublicImageUrl(String path) {
        if (path == null || path.isBlank() || path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            return path;
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return path;
        }
        return publicBaseUrl.replaceAll("/+$", "") + "/" + path.replaceFirst("^/+", "");
    }

    private String createFallbackReply(List<SuggestedListing> suggested) {
        if (suggested.isEmpty()) {
            return "Xin lỗi, mình chưa tìm thấy thông tin phù hợp.";
        }
        return "Mình tìm được " + suggested.size() + " cơ sở phù hợp với yêu cầu của bạn:";
    }

    private ParsedIntent parseUserIntent(String message) {
        String lower = message.toLowerCase();
        ParsedIntent intent = new ParsedIntent();

        // 1. Nhan dien loai co so
        if (lower.contains("villa") || lower.contains("biệt thự")) {
            intent.propertyTypeKeyword = "villa";
        } else if (lower.contains("resort") || lower.contains("nghỉ dưỡng")) {
            intent.propertyTypeKeyword = "resort";
        } else if (lower.contains("homestay")) {
            intent.propertyTypeKeyword = "homestay";
        } else if (lower.contains("khách sạn") || lower.contains("hotel") || lower.contains("ks")) {
            intent.propertyTypeKeyword = "khách sạn";
        } else if (lower.contains("căn hộ") || lower.contains("apartment")) {
            intent.propertyTypeKeyword = "căn hộ";
        }

        // 2. Nhan dien muc gia: "duoi 2 trieu", "duoi 2tr", "< 1500k", "tam 1tr"
        Pattern pTrieu = Pattern.compile("(?:dưới|<|tầm|khoảng)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr|trieu)");
        Matcher mTrieu = pTrieu.matcher(lower);
        if (mTrieu.find()) {
            try {
                double val = Double.parseDouble(mTrieu.group(1).replace(",", "."));
                intent.maxPrice = (long) (val * 1_000_000);
            } catch (Exception ignored) {
            }
        } else {
            Pattern pK = Pattern.compile("(?:dưới|<)?\\s*(\\d+)\\s*(?:k|nghìn|ngàn)");
            Matcher mK = pK.matcher(lower);
            if (mK.find()) {
                try {
                    long val = Long.parseLong(mK.group(1));
                    intent.maxPrice = val * 1_000;
                } catch (Exception ignored) {
                }
            } else {
                Pattern pDong = Pattern.compile("(?:dưới|<)?\\s*(\\d{1,3}(?:[.,]\\d{3})+)\\s*(?:đ|đồng|vnd)");
                Matcher mDong = pDong.matcher(lower);
                if (mDong.find()) {
                    try {
                        intent.maxPrice = Long.parseLong(mDong.group(1).replace(".", "").replace(",", ""));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 3. Nhan dien dia diem / view
        List<String> locTerms = List.of("view biển", "sát biển", "gần biển", "biển", "trần phú",
                "lộc thọ", "phước hải", "vĩnh phước", "trung tâm", "chợ đêm");
        for (String term : locTerms) {
            if (lower.contains(term)) {
                intent.locationKeywords.add(term);
            }
        }

        // 4. Nhan dien tien ich
        List<String> amTerms = List.of("hồ bơi", "bể bơi", "wifi", "bếp", "bãi đỗ xe", "đậu xe", "điều hòa");
        for (String term : amTerms) {
            if (lower.contains(term)) {
                intent.amenityKeywords.add(term);
            }
        }

        // 5. Nhan dien yeu cau ngoai pham vi Nha Trang
        List<String> outOfScopeKeywords = List.of("đà lạt", "da lat", "hà nội", "ha noi", "sài gòn", "sai gon",
                "hồ chí minh", "ho chi minh", "đà nẵng", "da nang", "vũng tàu", "vung tau", "phú quốc", "phu quoc",
                "sa pa", "sapa");
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

    private static class ParsedIntent {
        boolean isOutOfScope;
        boolean isApplicationComparison;
        String propertyTypeKeyword;
        Long maxPrice;
        List<String> locationKeywords = new ArrayList<>();
        List<String> amenityKeywords = new ArrayList<>();

        boolean hasSearchCriteria() {
            return propertyTypeKeyword != null || maxPrice != null
                    || !locationKeywords.isEmpty() || !amenityKeywords.isEmpty();
        }
    }

    private record ScoredListing(CoSoLuuTru listing, int score) {
    }
}
