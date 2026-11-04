package com.hotelnt.modules.admin;

import java.security.Principal;

public record AdminPrincipal(Integer accountId, String email, String role) implements Principal {
    @Override
    public String getName() {
        return email != null ? email : String.valueOf(accountId);
    }
}
