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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chi goi sau khi thong tin phien hoac tai khoan da duoc xac thuc. */
@Service
public class SessionRevocationService {
    private final SessionRevocationRepository sessions;

    public SessionRevocationService(SessionRevocationRepository sessions) {
        this.sessions = sessions;
    }

    @Transactional
    int revokeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("Missing session ID");
        return sessions.revokeSession(sessionId);
    }

    @Transactional
    int revokeAccountSessions(int accountId) {
        if (accountId <= 0) throw new IllegalArgumentException("Invalid account ID");
        return sessions.revokeAccountSessions(accountId);
    }
}
