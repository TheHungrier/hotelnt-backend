package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupVerificationService {
    private final AuthChallengeRepository challenges;
    private final OtpHasher otpHasher;
    private final SignupDuplicateChecker duplicates;
    private final TaiKhoanRepository accounts;
    private final SessionIssuer sessions;
    private final ObjectMapper json;

    public SignupVerificationService(AuthChallengeRepository challenges, OtpHasher otpHasher,
            SignupDuplicateChecker duplicates, TaiKhoanRepository accounts, SessionIssuer sessions, ObjectMapper json) {
        this.challenges = challenges;
        this.otpHasher = otpHasher;
        this.duplicates = duplicates;
        this.accounts = accounts;
        this.sessions = sessions;
        this.json = json;
    }

    public enum Status {
        SUCCESS, EXPIRED, INVALID, ATTEMPTS_EXCEEDED, DUPLICATE
    }

    public record UserInfo(Integer id, String hoTen, String email, String soDienThoai, String vaiTro) {
    }

    public record Response(String accessToken, String refreshToken, long expiresIn, UserInfo userInfo) {
    }

    public record Result(Status status, Response response, Object details) {
    }

    @Transactional
    public Result verify(SignupVerifyRequest request) {
        OtpChallenge challenge = challenges.findForUpdate(request.registrationId()).orElse(null);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (challenge == null || !"SIGNUP".equals(challenge.getLoai()) || Boolean.TRUE.equals(challenge.getDaDung())
                || OtpTiming.isExpired(challenge.getHetHanLuc(), now)) {
            return new Result(Status.EXPIRED, null, null);
        }
        int failures = challenge.getSoLanSai() == null ? 0 : challenge.getSoLanSai();
        if (failures >= 5)
            return new Result(Status.ATTEMPTS_EXCEEDED, null, null);
        if (!otpHasher.matches(request.otpCode(), challenge.getOtpHash())) {
            int remaining = 5 - ++failures;
            challenge.setSoLanSai(failures);
            if (remaining == 0)
                return new Result(Status.ATTEMPTS_EXCEEDED, null, null);
            return new Result(Status.INVALID, null,
                    List.of(Map.of("field", "attemptsLeft", "message", Integer.toString(remaining))));
        }
        SignupService.PendingSignup pending = readPending(challenge.getDuLieuDangKy());
        SignupRequest input = new SignupRequest(pending.hoTen(), pending.soDienThoai(), pending.email(), pending.cccd(),
                pending.matKhau());
        List<String> fields = duplicates.findDuplicateFields(input);
        if (!fields.isEmpty())
            return new Result(Status.DUPLICATE, null,
                    fields.stream().map(field -> Map.of("field", field, "message", "Đã được sử dụng")).toList());
        TaiKhoan account = new TaiKhoan();
        account.setVaiTro("HOST");
        account.setTrangThai("ACTIVE");
        account.setHoTen(pending.hoTen());
        account.setSoDienThoai(pending.soDienThoai());
        account.setEmail(pending.email());
        account.setCccd(pending.cccd());
        account.setMatKhau(pending.matKhau());
        account = accounts.saveAndFlush(account);
        SessionIssuer.TokenPair pair = sessions.issue(account);
        challenge.setDaDung(true);
        challenge.setDuLieuDangKy(null);
        return new Result(Status.SUCCESS, new Response(pair.accessToken(), pair.refreshToken(), pair.expiresIn(),
                new UserInfo(account.getId(), account.getHoTen(), account.getEmail(), account.getSoDienThoai(),
                        account.getVaiTro())),
                null);
    }

    private SignupService.PendingSignup readPending(String raw) {
        try {
            return json.readValue(raw, SignupService.PendingSignup.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid signup challenge", ex);
        }
    }
}
