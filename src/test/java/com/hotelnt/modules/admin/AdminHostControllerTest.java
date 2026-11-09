package com.hotelnt.modules.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.modules.admin.AdminDtos.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminHostController.class)
class AdminHostControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminHostService service;

    @MockBean
    private AdminIdentityResolver identities;

    @BeforeEach
    void setupAuth() {
        when(identities.resolve(any())).thenReturn(new AdminIdentityResolver.Identity(1, "admin@hotelnt.local", "ADMIN"));
    }

    @Test
    void missingToken_returns401() throws Exception {
        when(identities.resolve(any())).thenThrow(new AppException(ErrorCode.TOKEN_INVALID));

        mvc.perform(get("/admin/hosts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));

        verifyNoInteractions(service);
    }

    @Test
    void wrongRole_returns403() throws Exception {
        when(identities.resolve(any())).thenThrow(new AppException(ErrorCode.FORBIDDEN_ROLE));

        mvc.perform(get("/admin/hosts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));

        verifyNoInteractions(service);
    }

    @Test
    void getHosts_success() throws Exception {
        HostListItem item = new HostListItem(
                3, "Nguyễn Văn A", "host01@example.com", "0905123456", "ACTIVE", 3, null, Instant.now()
        );
        Pagination pagination = new Pagination(1, 1, 1, 10);
        when(service.getHosts(eq("ACTIVE"), eq("Nguyễn"), eq(1), eq(10)))
                .thenReturn(new HostPageResponse(List.of(item), pagination));

        mvc.perform(get("/admin/hosts")
                        .param("trangThai", "ACTIVE")
                        .param("keyword", "Nguyễn")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(3))
                .andExpect(jsonPath("$.items[0].hoTen").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.pagination.totalItems").value(1));
    }

    @Test
    void getHostDetail_success() throws Exception {
        HostDetail detail = new HostDetail(
                3, "Nguyễn Văn A", "host01@example.com", "0905123456", "079******234",
                "ACTIVE", null, null, Instant.now(), 3,
                Map.of("DRAFT", 1L, "PENDING", 0L, "APPROVED", 2L, "REJECTED", 0L, "HIDDEN", 0L)
        );
        when(service.getHostDetail(3)).thenReturn(detail);

        mvc.perform(get("/admin/hosts/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.cccd").value("079******234"))
                .andExpect(jsonPath("$.thongKeCSLT.APPROVED").value(2));
    }

    @Test
    void getHostDetail_notFound_returns404() throws Exception {
        when(service.getHostDetail(999)).thenThrow(new AppException(ErrorCode.HOST_NOT_FOUND));

        mvc.perform(get("/admin/hosts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("HOST_NOT_FOUND"));
    }

    @Test
    void lockHost_success() throws Exception {
        UpdateHostStatusResponse res = new UpdateHostStatusResponse(3, "LOCKED", "Vi phạm chính sách", Instant.now());
        when(service.updateHostStatus(eq(3), any())).thenReturn(res);

        mvc.perform(patch("/admin/hosts/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"LOCKED\",\"lyDoKhoa\":\"Vi phạm chính sách\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.trangThai").value("LOCKED"))
                .andExpect(jsonPath("$.lyDoKhoa").value("Vi phạm chính sách"));
    }

    @Test
    void unlockHost_success() throws Exception {
        UpdateHostStatusResponse res = new UpdateHostStatusResponse(3, "ACTIVE", null, Instant.now());
        when(service.updateHostStatus(eq(3), any())).thenReturn(res);

        mvc.perform(patch("/admin/hosts/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.trangThai").value("ACTIVE"))
                .andExpect(jsonPath("$.lyDoKhoa").doesNotExist());
    }

    @Test
    void updateStatus_invalidStatus_returns400() throws Exception {
        mvc.perform(patch("/admin/hosts/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void lockHost_stateUnchanged_returns409() throws Exception {
        when(service.updateHostStatus(eq(3), any())).thenThrow(new AppException(ErrorCode.STATE_UNCHANGED));

        mvc.perform(patch("/admin/hosts/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"LOCKED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STATE_UNCHANGED"));
    }

    @Test
    void lockHost_adminTarget_returns403() throws Exception {
        when(service.updateHostStatus(eq(1), any())).thenThrow(new AppException(ErrorCode.CANNOT_LOCK_ADMIN));

        mvc.perform(patch("/admin/hosts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"LOCKED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CANNOT_LOCK_ADMIN"));
    }
}
