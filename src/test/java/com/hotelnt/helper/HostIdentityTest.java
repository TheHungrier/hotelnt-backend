package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HostIdentityTest {
    HostIdentityResolver resolver = new HostConfiguration().hostIdentityResolver();

    @Test
    void userSuppliedHeadersDoNotAuthenticate() {
        var req = new MockHttpServletRequest();
        req.addHeader("X-Host-Id", "1");
        req.addHeader("Authorization", "Bearer unsigned-payload");
        assertEquals(ErrorCode.TOKEN_INVALID, assertThrows(AppException.class, () -> resolver.resolve(req)).getCode());
    }

    @Test
    void arbitraryPrincipalNameDoesNotAuthenticate() {
        var req = new MockHttpServletRequest();
        req.setUserPrincipal(() -> "1");
        assertThrows(AppException.class, () -> resolver.resolve(req));
    }

    @Test
    void acceptsOnlyTrustedAuthPrincipal() {
        var req = new MockHttpServletRequest();
        req.setUserPrincipal(new HostPrincipal() {
            public Integer accountId() {
                return 7;
            }

            public String sessionId() {
                return "session";
            }

            public String getName() {
                return "7";
            }
        });
        assertEquals(7, resolver.resolve(req).accountId());
        assertEquals("session", resolver.resolve(req).sessionId());
    }
}