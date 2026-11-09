package com.hotelnt.modules.auth;

import com.hotelnt.domain.TaiKhoan;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Duoc goi trong transaction de khoa tai khoan va luu so lan dang nhap sai. */
@Component
class CredentialsChecker {
    private final AuthAccountRepository accounts;
    private final PasswordHasher passwords;

    CredentialsChecker(AuthAccountRepository accounts, PasswordHasher passwords) {
        this.accounts = accounts; this.passwords = passwords;
    }

    enum Status { SUCCESS, INVALID, TEMP_LOCKED, LOCKED }
    record Result(Status status, TaiKhoan account, Object details) {}

    Result check(String identity, String password, String role, LocalDateTime now) {
        TaiKhoan account = accounts.findForUpdate(identity).orElse(null);
        if (account == null || !role.equals(account.getVaiTro())
                || ("ADMIN".equals(role) && !identity.equalsIgnoreCase(account.getEmail()))) {
            return new Result(Status.INVALID, null, null);
        }
        if (account.getKhoaDen() != null && now.isBefore(account.getKhoaDen())) {
            long seconds = Math.max(1, Duration.between(now, account.getKhoaDen()).toSeconds() + 1);
            return new Result(Status.TEMP_LOCKED, null, List.of(Map.of("field", "retryAfter", "message", Long.toString(seconds))));
        }
        if (account.getKhoaDen() != null) { account.setKhoaDen(null); account.setSoLanDangNhapSai(0); }
        if (!passwords.matches(password, account.getMatKhau())) {
            int failures = (account.getSoLanDangNhapSai() == null ? 0 : account.getSoLanDangNhapSai()) + 1;
            account.setSoLanDangNhapSai(failures);
            if (failures >= 5) {
                account.setKhoaDen(now.plusMinutes(15));
                return new Result(Status.TEMP_LOCKED, null, List.of(Map.of("field", "retryAfter", "message", "900")));
            }
            return new Result(Status.INVALID, null, null);
        }
        if (!"ACTIVE".equals(account.getTrangThai())) return new Result(Status.LOCKED, null, null);
        account.setSoLanDangNhapSai(0);
        return new Result(Status.SUCCESS, account, null);
    }
}
