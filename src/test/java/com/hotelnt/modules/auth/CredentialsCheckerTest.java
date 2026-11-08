package com.hotelnt.modules.auth;

import com.hotelnt.domain.TaiKhoan;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredentialsCheckerTest {
    private final AuthAccountRepository repository = mock(AuthAccountRepository.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final CredentialsChecker checker = new CredentialsChecker(repository, passwords);
    @Test
    void locksAccountAfterFiveIncorrectPasswords() {
        TaiKhoan account = new TaiKhoan(); account.setVaiTro("HOST"); account.setTrangThai("ACTIVE");
        account.setMatKhau("hash"); account.setSoLanDangNhapSai(4);
        when(repository.findForUpdate("identity")).thenReturn(Optional.of(account));
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
        assertEquals(CredentialsChecker.Status.TEMP_LOCKED, checker.check("identity", "bad", "HOST", now).status());
        assertEquals(now.plusMinutes(15), account.getKhoaDen());
    }
}
