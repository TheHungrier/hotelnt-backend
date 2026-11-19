package com.hotelnt.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.domain.PhienDangNhap;
import com.hotelnt.domain.TaiKhoan;
import com.hotelnt.dto.AdminDtos.ModerateListingRequest;
import com.hotelnt.dto.AdminDtos.UpdateHostStatusRequest;
import com.hotelnt.dto.HostDtos.Contact;
import com.hotelnt.dto.HostDtos.ListingWrite;
import com.hotelnt.dto.SignupRequest;
import com.hotelnt.dto.SignupVerifyRequest;
import com.hotelnt.helper.OtpEmailSender;
import com.hotelnt.repository.PhienDangNhapRepository;
import com.hotelnt.repository.TaiKhoanRepository;
import com.hotelnt.service.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FullListingLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private AuthTokenService tokenService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepo;

    @Autowired
    private PhienDangNhapRepository sessionRepo;

    @MockBean
    private OtpEmailSender emailSender;

    @Test
    @DisplayName("Luồng đầy đủ: Đăng ký Host -> Tạo cơ sở -> Gửi duyệt -> Admin duyệt -> Xuất hiện public -> Khóa Host -> Ẩn public")
    void testFullListingLifecycle() throws Exception {
        // 1. Host đăng ký tài khoản qua /auth/signup
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String email = "host_" + uniqueId + "@example.com";
        String phone = "09" + String.format("%08d", Math.abs(uniqueId.hashCode() % 100000000));
        String cccd = "079201" + String.format("%06d", Math.abs(uniqueId.hashCode() % 1000000));

        SignupRequest signupReq = new SignupRequest("Nguyen Host Lifecycle", phone, email, cccd, "Pass@1234!");
        String signupResp = mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(signupReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(anyString(), otpCaptor.capture());
        String otp = otpCaptor.getValue();
        String registrationId = json.readTree(signupResp).get("registrationId").asText();

        // 2. Xác minh OTP để hoàn tất đăng ký và lấy access token
        SignupVerifyRequest verifyReq = new SignupVerifyRequest(registrationId, otp);
        String verifyResp = mvc.perform(post("/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode authData = json.readTree(verifyResp);
        String hostToken = authData.get("accessToken").asText();
        int hostId = authData.get("account").get("id").asInt();

        // 3. Host tạo cơ sở lưu trú mới (Trạng thái DRAFT)
        ListingWrite createReq = new ListingWrite(
                "Lifecycle Beach Villa " + uniqueId,
                2,
                1,
                "123 Duong Bien, Nha Trang",
                "Villa sang trong",
                new Contact("0905123456", "https://zalo.me/0905123456", null),
                BigDecimal.valueOf(1_500_000L),
                List.of(1, 2),
                BigDecimal.valueOf(12.2451),
                BigDecimal.valueOf(109.1943)
        );

        String createResp = mvc.perform(post("/host/listings")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trangThai").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        int maCSLT = json.readTree(createResp).get("maCSLT").asInt();

        // 4. Kiểm tra: cơ sở DRAFT KHÔNG xuất hiện ở public API
        mvc.perform(get("/listings/" + maCSLT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));

        String publicListDraft = mvc.perform(get("/listings?keyword=" + uniqueId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(publicListDraft.contains("Lifecycle Beach Villa " + uniqueId));

        // 5. Host gửi duyệt cơ sở lưu trú -> Trạng thái PENDING
        mvc.perform(post("/host/listings/" + maCSLT + "/submit-review")
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("PENDING"));

        // 6. Kiểm tra: cơ sở PENDING vẫn KHÔNG xuất hiện ở public API
        mvc.perform(get("/listings/" + maCSLT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));

        // 7. Admin duyệt cơ sở lưu trú -> Trạng thái APPROVED
        TaiKhoan admin = taiKhoanRepo.findByEmail("admin@hotelnt.local").orElseGet(() -> {
            TaiKhoan a = new TaiKhoan();
            a.setEmail("admin@hotelnt.local");
            a.setHoTen("Admin Master");
            a.setVaiTro("ADMIN");
            a.setTrangThai("ACTIVE");
            a.setMatKhau("secret");
            return taiKhoanRepo.save(a);
        });

        PhienDangNhap adminSession = new PhienDangNhap();
        adminSession.setMaPhien(UUID.randomUUID().toString());
        adminSession.setTaiKhoan(admin);
        adminSession.setDaThuHoi(false);
        adminSession.setNgayTao(LocalDateTime.now());
        adminSession.setHetHanLuc(LocalDateTime.now().plusDays(1));
        sessionRepo.save(adminSession);

        String adminToken = tokenService.accessToken(admin.getId(), adminSession.getMaPhien());
        ModerateListingRequest approveReq = new ModerateListingRequest("APPROVE", null);

        mvc.perform(patch("/admin/listings/" + maCSLT + "/moderation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("APPROVED"));

        // 8. Kiểm tra: cơ sở APPROVED xuất hiện ở GET /listings và GET /listings/{id}
        mvc.perform(get("/listings/" + maCSLT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maCSLT").value(maCSLT))
                .andExpect(jsonPath("$.tenCSLT").value("Lifecycle Beach Villa " + uniqueId));

        String publicListApproved = mvc.perform(get("/listings?keyword=" + uniqueId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(publicListApproved.contains("Lifecycle Beach Villa " + uniqueId));

        // 9. Admin khóa tài khoản Host -> Cơ sở bị ẩn khỏi public API
        UpdateHostStatusRequest lockHostReq = new UpdateHostStatusRequest("LOCKED", "Vi phạm chính sách nội dung");
        mvc.perform(patch("/admin/hosts/" + hostId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(lockHostReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("LOCKED"));

        // 10. Kiểm tra: khi Host bị khóa, cơ sở KHÔNG còn xuất hiện ở public API
        mvc.perform(get("/listings/" + maCSLT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));

        String publicListLocked = mvc.perform(get("/listings?keyword=" + uniqueId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(publicListLocked.contains("Lifecycle Beach Villa " + uniqueId));
    }
}
