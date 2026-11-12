package com.hotelnt.service;

import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
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

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private CoSoLuuTruRepository listingRepo;

    @Mock
    private ChatbotGeminiClient geminiClient;

    private ChatbotService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotService(listingRepo, geminiClient);
    }

    private CoSoLuuTru createMockListing(int id, String name, String typeName, BigDecimal price, String wardName) {
        CoSoLuuTru c = new CoSoLuuTru();
        c.setMaCSLT(id);
        c.setTenCSLT(name);
        c.setGia(price);
        c.setDiaChiChiTiet("12 Tr?n Ph�");
        c.setMoTa("View bi?n tuy?t d?p");
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
        host.setHoTen("Nguy?n Van A");
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
    void handleMessage_matchingQuery_returnsSuggestionsWithGeminiReply() {
        CoSoLuuTru l1 = createMockListing(1, "Villa View Bi?n Nha Trang", "Villa", BigDecimal.valueOf(1500000), "Phu?c H?i");
        CoSoLuuTru l2 = createMockListing(2, "Kh�ch s?n Sao Bi?n", "Kh�ch s?n", BigDecimal.valueOf(800000), "L?c Th?");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1, l2));
        when(geminiClient.generateReply(anyString(), anyList()))
                .thenReturn("D?, HotelNT g?i � cho b?n Villa View Bi?n Nha Trang r?t th�ch h?p cho k? ngh? ?!");

        ChatMessageRequest req = new ChatMessageRequest("T�m cho t�i villa view bi?n gi� du?i 2 tri?u", "session-01");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-01");
        assertThat(res.replyText()).contains("Villa View Bi?n Nha Trang");
        assertThat(res.suggestedListings()).isNotEmpty();
        assertThat(res.suggestedListings().get(0).maCSLT()).isEqualTo(1);
        assertThat(res.suggestedListings().get(0).tenLoai()).isEqualTo("Villa");
    }

    @Test
    void handleMessage_noMatch_returnsEmptySuggestionsWithFallback() {
        CoSoLuuTru l1 = createMockListing(1, "Villa Cao C?p", "Villa", BigDecimal.valueOf(10000000), "Phu?c H?i");

        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of(l1));
        when(geminiClient.generateReply(anyString(), anyList())).thenReturn(null); // Gemini fallback

        ChatMessageRequest req = new ChatMessageRequest("T�m ph�ng gi� du?i 100k", "session-02");
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isEqualTo("session-02");
        assertThat(res.suggestedListings()).isEmpty();
        assertThat(res.replyText()).contains("Xin l?i, m�nh chua t�m th?y th�ng tin ph� h?p");
    }

    @Test
    void handleMessage_noSessionId_generatesNewSessionId() {
        when(listingRepo.findAll(any(Specification.class))).thenReturn(List.of());
        when(geminiClient.generateReply(anyString(), anyList())).thenReturn(null);

        ChatMessageRequest req = new ChatMessageRequest("Ch�o b?n", null);
        ChatMessageResponse res = service.handleMessage(req);

        assertThat(res.sessionId()).isNotNull().isNotBlank();
    }
}