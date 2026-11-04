package com.hotelnt.modules.admin;

import jakarta.servlet.http.HttpServletRequest;

public interface AdminIdentityResolver {
    record Identity(Integer accountId, String email, String role) {}
    Identity resolve(HttpServletRequest request);
}
