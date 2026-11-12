package com.hotelnt.helper;

import jakarta.servlet.http.HttpServletRequest;

public interface AdminIdentityResolver {
    public record Identity(Integer accountId, String email, String role) {
    }

    Identity resolve(HttpServletRequest request);
}
