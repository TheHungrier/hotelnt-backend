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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogoutServiceTest {
    private final AuthTokenService tokens = new AuthTokenService("01234567890123456789012345678901", 604800);
    private final AuthSessionRepository repository = mock(AuthSessionRepository.class);
    private final LogoutService service = new LogoutService(tokens, repository);

    @Test
    void revokesSessionAndAllowsRepeatedLogout() {
        String refresh = tokens.refreshToken(42, "session-id");
        String access = "Bearer " + tokens.accessToken(42, "session-id");
        TaiKhoan account = new TaiKhoan(); account.setId(42);
        PhienDangNhap session = new PhienDangNhap(); session.setMaPhien("session-id"); session.setTaiKhoan(account);
        session.setRefreshTokenHash(SessionIssuer.hash(refresh));
        session.setHetHanLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1));
        when(repository.findForUpdate("session-id")).thenReturn(Optional.of(session));

        assertTrue(service.logout(access, refresh));
        assertTrue(session.getDaThuHoi());
        assertTrue(service.logout(access, refresh));
        assertFalse(service.logout(null, refresh));
    }
}