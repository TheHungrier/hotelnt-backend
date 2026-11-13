package com.hotelnt.service;

import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.domain.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private CoSoLuuTruRepository listingRepo;

    @Mock
    private ChatbotAiClient aiClient;

    private ChatbotService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotService(listingRepo, aiClient);
    }

    private CoSoLuuTru createMockListing(int id, String name, String typeName, BigDecimal price, String wardName) {
        CoSoLuuTru c = new CoSoLuuTru();
        c.setMaCSLT(id);
        c.setTenCSLT(name);
        c.setGia(price);
        c.setDiaChiChiTiet("12 Trần Phú");
        c.setMoTa("View biển tuyệt đẹp");
        c.setTrangThai("APPROVED");
        c.setNgayTao(LocalDateTime.now());

        LoaiCoSo loai = new LoaiCoSo();
        loai.setMaLoai(1);
        loai.setTenLoai(typeName);
        c.setLoai(loai);

        Phuong p = new Phuong();
        p.setMaPhuong(1);
        p.setTenPhuong(wardName);
        c.setPhuong(p);

        TaiKhoan host = new TaiKhoan();
        host.setId(2);
        host.setHoTen("Nguyễn Văn A");
        host.setTrangThai("ACTIVE");
        c.setHost(host);

        Media m = new Media();
        m.setMediaId(1);
        m.setDuongDan("/uploads/1.jpg");
        m.setLoai("IMAGE");
        m.setThuTu(1);
        c.setMedia(List.of(m));

        LienHe lh = new LienHe();
        lh.setMaLienHe(1);
        lh.setLoaiLienHe("SO_DIEN_THOAI");
        lh.setGiaTri("0901234567");
        c.setLienHe(List.of(lh));

        return c;
    }

    @Test
    void handleMessage_matchingQuery_returnsSuggestionsWithAiReply() {
        CoSoLuuTru l1 = createMockListing(1, "Villa View Biển Nha Trang", "Villa", BigDecimal.valueOf(1500000),
                "Phước Hải");
        CoSoLuuTru l2 = createMockListing(2, "Khách sạn Sao Biển", "Khách sạn", BigDecimal.valueOf(800000), "Lộc Thọ");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1, l2));
        when(aiClient.generateReply(anyString(), anyList()))
                .thenReturn("Dạ, HotelNT gợi ý cho bạn Villa View Biển Nha Trang rất thích hợp cho kỳ nghỉ!");

        ChatMessageRequest req = new ChatMessageRequest("Tìm cho tôi villa view biển giá dưới 2 triệu", "session-01");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-01");
        assertThat(res.replyText()).contains("Villa View Biển Nha Trang");
        assertThat(res.suggestedListings()).isNotEmpty();
        assertThat(res.suggestedListings().get(0).maCSLT()).isEqualTo(1);
        assertThat(res.suggestedListings().get(0).tenLoai()).isEqualTo("Villa");
    }

    @Test
    void handleMessage_noMatch_returnsEmptySuggestionsWithFallback() {
        CoSoLuuTru l1 = createMockListing(1, "Villa Cao Cấp", "Villa", BigDecimal.valueOf(10000000), "Phước Hải");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(null); // AI fallback

        ChatMessageRequest req = new ChatMessageRequest("Tìm phòng giá dưới 100k", "session-02");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-02");
        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("Xin lỗi, mình chưa tìm thấy thông tin phù hợp");
    }

    @Test
    void handleMessage_noSessionId_generatesNewSessionId() {
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of());
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(null);

        ChatMessageRequest req = new ChatMessageRequest("Chào bạn", null);
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isNotNull().isNotBlank();
    }

    @Test
    void handleMessage_priceAndTypeAreHardFilters() {
        CoSoLuuTru hotelOverBudget = createMockListing(1, "Khách sạn Sao Biển", "Khách sạn",
                BigDecimal.valueOf(800000), "Lộc Thọ");
        CoSoLuuTru homestayInBudget = createMockListing(2, "Homestay Nắng Mai", "Homestay",
                BigDecimal.valueOf(450000), "Lộc Thọ");
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(hotelOverBudget, homestayInBudget));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(null);

        ChatMessageResponse res = service.handleMessage(
                new ChatMessageRequest("Khách sạn giá dưới 500k", "session-price"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("chưa tìm thấy");
    }

    @Test
    void handleMessage_withoutSearchCriteria_doesNotReturnDefaultTopFive() {
        CoSoLuuTru listing = createMockListing(1, "Villa Nha Trang", "Villa",
                BigDecimal.valueOf(1500000), "Phước Hải");
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(listing));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(null);

        ChatMessageResponse res = service.handleMessage(new ChatMessageRequest("Bạn khỏe không?", "session-general"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("chưa tìm thấy");
    }

    @Test
    void handleMessage_prefixesRelativeImageWithConfiguredPublicOrigin() {
        CoSoLuuTru listing = createMockListing(1, "Villa Nha Trang", "Villa",
                BigDecimal.valueOf(1500000), "Phước Hải");
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(listing));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(null);
        ReflectionTestUtils.setField(service, "publicBaseUrl", "https://hotelnt.example.com/");

        ChatMessageResponse res = service.handleMessage(new ChatMessageRequest("Tìm villa", "session-image"));

        assertThat(res.suggestedListings().get(0).duongDanAnh())
                .isEqualTo("https://hotelnt.example.com/uploads/1.jpg");
    }

    @Test
    void handleMessage_applicationComparisonUsesHotelntResponse() {
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of());

        ChatMessageResponse res = service.handleMessage(
                new ChatMessageRequest("Ứng dụng chọn khách sạn tốt nhất là gì?", "session-app"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("HotelNT");
    }
}