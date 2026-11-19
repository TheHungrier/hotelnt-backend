package com.hotelnt.config;

import com.hotelnt.helper.*;
import com.hotelnt.service.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class HostConfiguration {
    public HostIdentityResolver hostIdentityResolver() {
        return request -> {
            if (!(request.getUserPrincipal() instanceof HostPrincipal p) || p.accountId() == null) {
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }
            return new HostIdentityResolver.Identity(p.accountId(), p.sessionId());
        };
    }

    @Bean
    @ConditionalOnMissingBean(HostIdentityResolver.class)
    public HostIdentityResolver hostIdentityResolver(AuthenticatedAccountService accounts) {
        return request -> {
            if (request.getUserPrincipal() instanceof HostPrincipal p) {
                if (p.accountId() == null) throw new AppException(ErrorCode.TOKEN_INVALID);
                return new HostIdentityResolver.Identity(p.accountId(), p.sessionId());
            }
            return accounts.resolve(request.getHeader("Authorization"), "HOST")
                    .map(identity -> new HostIdentityResolver.Identity(
                            identity.accountId(), identity.sessionId()))
                    .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
        };
    }

    @Bean
    BCryptPasswordEncoder hostPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
