package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {
    private final SignupDuplicateChecker duplicates;
    private final PasswordHasher passwords;
    private final OtpCodeGenerator codes;
    private final OtpHasher otpHasher;
    private final OtpChallengeRepository challenges;
    private final OtpEmailSender email;
    private final ObjectMapper json;

    public SignupService(SignupDuplicateChecker duplicates, PasswordHasher passwords, OtpCodeGenerator codes,
                  OtpHasher otpHasher, OtpChallengeRepository challenges, OtpEmailSender email, ObjectMapper json) {
        this.duplicates = duplicates;
        this.passwords = passwords;
        this.codes = codes;
        this.otpHasher = otpHasher;
        this.challenges = challenges;
        this.email = email;
        this.json = json;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PendingSignup(String hoTen, String soDienThoai, String email, String cccd, String matKhau) {}
    public record Response(String registrationId, int otpExpiresIn, int resendAfter, String message) {}

    @Transactional
    public Response signup(SignupRequest request) {
        List<String> fields = duplicates.findDuplicateFields(request);
        if (!fields.isEmpty()) {
            throw new AppException(ErrorCode.ACCOUNT_DUPLICATED,
                    fields.stream().map(field -> Map.of("field", field, "message", "Đã được sử dụng")).toList());
        }

        String otp = codes.generate();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        OtpChallenge challenge = new OtpChallenge();
        challenge.setChallengeId(UUID.randomUUID().toString());
        challenge.setLoai("SIGNUP");
        challenge.setEmail(request.email());
        challenge.setDuLieuDangKy(toJson(new PendingSignup(request.hoTen(), request.soDienThoai(),
                request.email(), request.cccd(), passwords.hash(request.matKhau()))));
        challenge.setOtpHash(otpHasher.hash(otp));
        challenge.setHetHanLuc(OtpTiming.expiresAt(now));
        challenge.setGuiLaiSauLuc(OtpTiming.resendAllowedAt(now));
        challenges.save(challenge);
        email.send(request.email(), otp);
        return new Response(challenge.getChallengeId(), OtpTiming.EXPIRES_IN_SECONDS,
                OtpTiming.RESEND_AFTER_SECONDS, "Đã gửi OTP");
    }

    private String toJson(PendingSignup signup) {
        try {
            return json.writeValueAsString(signup);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize signup", ex);
        }
    }
}
