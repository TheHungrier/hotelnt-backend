package com.hotelnt.repository;

import com.hotelnt.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuthChallengeRepository extends Repository<OtpChallenge, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.challengeId = :id")
    Optional<OtpChallenge> findForUpdate(@Param("id") String id);
}
