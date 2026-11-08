package com.hotelnt.modules.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chi goi sau khi thong tin phien hoac tai khoan da duoc xac thuc. */
@Service
class SessionRevocationService {
    private final SessionRevocationRepository sessions;

    SessionRevocationService(SessionRevocationRepository sessions) {
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
