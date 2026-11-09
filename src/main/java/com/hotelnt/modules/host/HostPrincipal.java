package com.hotelnt.modules.host;
import java.security.Principal;
/** Auth must expose this trusted identity (or replace HostIdentityResolver). Never populate from request headers/body. */
public interface HostPrincipal extends Principal {
    Integer accountId();
    String sessionId();
}
