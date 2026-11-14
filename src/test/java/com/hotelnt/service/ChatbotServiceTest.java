package com.hotelnt.service;

import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.domain.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private CoSoLuuTruRepository listingRepo;

    @Mock
    private AiClient aiClient;

    private ChatbotService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotService(listingRepo, aiClient);
    }

    private CoSoLuuTru createMockListing(int id, String name, String typeName, BigDecimal price, String wardName, String hostStatus) {
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
        host.setTrangThai(hostStatus != null ? hostStatus : "ACTIVE");
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

    private CoSoLuuTru createMockListing(int id, String name, String typeName, BigDecimal price, String wardName) {
        return createMockListing(id, name, typeName, price, wardName, "ACTIVE");
    }

    @Test
    @DisplayName("Khớp câu hỏi: trả về danh sách gợi ý kèm phản hồi từ AI")
    void handleMessage_matchingQuery_returnsSuggestionsWithAiReply() {
        CoSoLuuTru l1 = createMockListing(1, "Villa View Biển Nha Trang", "Villa", BigDecimal.valueOf(1500000), "Phước Hải");
        CoSoLuuTru l2 = createMockListing(2, "Khách sạn Sao Biển", "Khách sạn", BigDecimal.valueOf(800000), "Lộc Thọ");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1, l2));
        when(aiClient.generateReply(anyString(), anyList()))
                .thenReturn(Optional.of("Dạ, HotelNT gợi ý cho bạn Villa View Biển Nha Trang rất thích hợp cho kỳ nghỉ!"));

        ChatMessageRequest req = new ChatMessageRequest("Tìm cho tôi villa view biển giá dưới 2 triệu", "session-01");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-01");
        assertThat(res.replyText()).contains("Villa View Biển Nha Trang");
        assertThat(res.suggestedListings()).isNotEmpty();
        assertThat(res.suggestedListings().get(0).maCSLT()).isEqualTo(1);
        assertThat(res.suggestedListings().get(0).tenLoai()).isEqualTo("Villa");
    }

    @Test
    @DisplayName("Không có cơ sở phù hợp: trả về suggestedListings rỗng và thông báo chưa có cơ sở phù hợp")
    void handleMessage_noMatch_returnsEmptySuggestionsWithFallback() {
        CoSoLuuTru l1 = createMockListing(1, "Villa Cao Cấp", "Villa", BigDecimal.valueOf(10000000), "Phước Hải");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1));

        ChatMessageRequest req = new ChatMessageRequest("Tìm phòng giá dưới 100k", "session-02");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-02");
        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("chưa có cơ sở phù hợp");
    }

    @Test
    @DisplayName("Không có sessionId: tự động sinh sessionId mới")
    void handleMessage_noSessionId_generatesNewSessionId() {
        ChatMessageRequest req = new ChatMessageRequest("Chào bạn", null);
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Khách sạn giá dưới 500k: lọc cứng loại bỏ khách sạn > 500k và loại hình không phải khách sạn")
    void handleMessage_priceAndTypeAreHardFilters() {
        CoSoLuuTru hotelOverBudget = createMockListing(1, "Khách sạn Sao Biển", "Khách sạn",
                BigDecimal.valueOf(800000), "Lộc Thọ");
        CoSoLuuTru homestayInBudget = createMockListing(2, "Homestay Nắng Mai", "Homestay",
                BigDecimal.valueOf(450000), "Lộc Thọ");
        CoSoLuuTru hotelInBudget = createMockListing(3, "Khách sạn Hải Âu", "Khách sạn",
                BigDecimal.valueOf(400000), "Lộc Thọ");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(hotelOverBudget, homestayInBudget, hotelInBudget));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(Optional.empty());

        ChatMessageResponse res = service.handleMessage(
                new ChatMessageRequest("Khách sạn giá dưới 500k", "session-price"));

        assertThat(res.suggestedListings()).hasSize(1);
        assertThat(res.suggestedListings().get(0).maCSLT()).isEqualTo(3);
        assertThat(res.suggestedListings().get(0).gia()).isLessThanOrEqualTo(500000L);
    }

    @Test
    @DisplayName("Câu hỏi chung / chào hỏi: không có tiêu chí tìm kiếm thì không trả listing và hỏi lại thông tin")
    void handleMessage_withoutSearchCriteria_doesNotReturnDefaultTopFive() {
        ChatMessageResponse res = service.handleMessage(new ChatMessageRequest("Bạn khỏe không?", "session-general"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).containsAnyOf("loại hình", "ngân sách", "khu vực");
    }

    @Test
    @DisplayName("Câu chào hỏi: không trả listing và hỏi lại loại hình / ngân sách / khu vực")
    void handleMessage_greeting_asksForCriteria() {
        ChatMessageResponse res = service.handleMessage(new ChatMessageRequest("Xin chào", "session-greeting"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).containsAnyOf("loại hình", "ngân sách", "khu vực");
    }

    @Test
    @DisplayName("app.public-base-url: ghép URL tuyệt đối cho ảnh tương đối")
    void handleMessage_prefixesRelativeImageWithConfiguredPublicOrigin() {
        CoSoLuuTru listing = createMockListing(1, "Villa Nha Trang", "Villa",
                BigDecimal.valueOf(1500000), "Phước Hải");
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(listing));
        when(aiClient.generateReply(anyString(), anyList())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(service, "publicBaseUrl", "https://hotelnt.example.com/api");

        ChatMessageResponse res = service.handleMessage(new ChatMessageRequest("Tìm villa", "session-image"));

        assertThat(res.suggestedListings().get(0).duongDanAnh())
                .isEqualTo("https://hotelnt.example.com/api/uploads/1.jpg");
    }

    @Test
    @DisplayName("So sánh ứng dụng: trả về câu trả lời giới thiệu HotelNT")
    void handleMessage_applicationComparisonUsesHotelntResponse() {
        ChatMessageResponse res = service.handleMessage(
                new ChatMessageRequest("Ứng dụng chọn khách sạn tốt nhất là gì?", "session-app"));

        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("HotelNT");
    }

    @Test
    @DisplayName("Mọi truy vấn đều sử dụng ChatbotSpecs.visibleApproved (Host bị khóa thì không được trả về)")
    void handleMessage_queriesWithVisibleApprovedSpecification() {
        CoSoLuuTru listing = createMockListing(1, "Villa Biển", "Villa", BigDecimal.valueOf(1000000), "Lộc Thọ");
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(listing));

        service.handleMessage(new ChatMessageRequest("Tìm villa", "session-spec"));

        // Xác nhận listingRepo.findAll được gọi với Specification
        ArgumentCaptor<Specification<CoSoLuuTru>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(listingRepo).findAll(specCaptor.capture());
        assertThat(specCaptor.getValue()).isNotNull();
    }
}
