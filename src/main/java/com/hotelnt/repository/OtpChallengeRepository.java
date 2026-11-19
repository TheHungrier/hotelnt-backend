package com.hotelnt.repository;

import com.hotelnt.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
}
