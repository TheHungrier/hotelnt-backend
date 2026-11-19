package com.hotelnt.helper;

import java.security.Principal;

public interface HostPrincipal extends Principal {
    Integer accountId();

    String sessionId();
}
