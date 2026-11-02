package com.hotelnt.repository;

import com.hotelnt.domain.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
}
