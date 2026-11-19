package com.hotelnt.helper;

import com.hotelnt.domain.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Goi trong transaction va giu khoa dong cua challenge. */
@Component
public class ChallengeVerifier {
    private final OtpHasher hasher;

    public ChallengeVerifier(OtpHasher hasher) {
        this.hasher = hasher;
    }

    public enum Status {
        VALID, EXPIRED, INVALID, ATTEMPTS_EXCEEDED
    }

    public record Result(Status status, Object details) {
    }

    public Result check(OtpChallenge challenge, String expectedType, String code, LocalDateTime now) {
        if (challenge == null || !expectedType.equals(challenge.getLoai()) || Boolean.TRUE.equals(challenge.getDaDung())
                || OtpTiming.isExpired(challenge.getHetHanLuc(), now))
            return new Result(Status.EXPIRED, null);
        int failures = challenge.getSoLanSai() == null ? 0 : challenge.getSoLanSai();
        if (failures >= 5)
            return new Result(Status.ATTEMPTS_EXCEEDED, null);
        if (!hasher.matches(code, challenge.getOtpHash())) {
            int remaining = 5 - ++failures;
            challenge.setSoLanSai(failures);
            if (remaining == 0)
                return new Result(Status.ATTEMPTS_EXCEEDED, null);
            return new Result(Status.INVALID,
                    List.of(Map.of("field", "attemptsLeft", "message", Integer.toString(remaining))));
        }
        return new Result(Status.VALID, null);
    }
}
