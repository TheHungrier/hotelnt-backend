package com.hotelnt.helper;

import jakarta.servlet.http.HttpServletRequest;

public interface HostIdentityResolver {
    public record Identity(Integer accountId, String sessionId) {
    }

    Identity resolve(HttpServletRequest request);
}
