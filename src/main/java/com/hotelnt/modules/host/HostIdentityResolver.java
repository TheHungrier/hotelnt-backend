package com.hotelnt.modules.host;
import jakarta.servlet.http.HttpServletRequest;
public interface HostIdentityResolver {
    record Identity(Integer accountId, String sessionId) {}
    Identity resolve(HttpServletRequest request);
}
