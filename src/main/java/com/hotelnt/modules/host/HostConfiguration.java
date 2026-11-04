package com.hotelnt.modules.host;
import com.hotelnt.common.error.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Configuration
public class HostConfiguration {
    @Bean @ConditionalOnMissingBean(HostIdentityResolver.class)
    HostIdentityResolver hostIdentityResolver() {
        return request -> {
            if (!(request.getUserPrincipal() instanceof HostPrincipal p) || p.accountId() == null)
                throw new AppException(ErrorCode.TOKEN_INVALID);
            return new HostIdentityResolver.Identity(p.accountId(), p.sessionId());
        };
    }
    @Bean BCryptPasswordEncoder hostPasswordEncoder() { return new BCryptPasswordEncoder(); }
}
