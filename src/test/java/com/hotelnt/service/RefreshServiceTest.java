package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshServiceTest {
    private final AuthTokenService tokens = new AuthTokenService("01234567890123456789012345678901", 604800);
    private final AuthSessionRepository repository = mock(AuthSessionRepository.class);
    private final RefreshService service = new RefreshService(tokens, repository);

    @Test
    void rotatesRefreshTokenAndRevokesSessionOnReuse() {
        String token = tokens.refreshToken(42, "session-id");
        TaiKhoan account = new TaiKhoan(); account.setId(42); account.setTrangThai("ACTIVE");
        PhienDangNhap session = new PhienDangNhap(); session.setMaPhien("session-id"); session.setTaiKhoan(account);
        session.setRefreshTokenHash(SessionIssuer.hash(token));
        session.setHetHanLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1));
        when(repository.findForUpdate("session-id")).thenReturn(Optional.of(session));

        RefreshService.Result result = service.refresh(token);
        assertEquals(RefreshService.Status.SUCCESS, result.status());
        assertNotEquals(token, result.pair().refreshToken());
        assertEquals(RefreshService.Status.INVALID, service.refresh(token).status());
        assertTrue(session.getDaThuHoi());
    }

    @Test
    void rejectsInvalidToken() {
        assertEquals(RefreshService.Status.INVALID, service.refresh("invalid").status());
    }
}