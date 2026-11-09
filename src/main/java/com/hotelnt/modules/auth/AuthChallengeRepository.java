package com.hotelnt.modules.auth;

import com.hotelnt.domain.OtpChallenge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface AuthChallengeRepository extends Repository<OtpChallenge, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.challengeId = :id")
    Optional<OtpChallenge> findForUpdate(@Param("id") String id);
}
