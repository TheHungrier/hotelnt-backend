package com.hotelnt.modules.admin;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.modules.auth.AuthenticatedAccountService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(tags = {
        @Tag(name = "admin-host-controller"),
        @Tag(name = "admin-listing-controller"),
        @Tag(name = "chatbot-controller")
})
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class AdminConfiguration {

    @Bean
    @ConditionalOnMissingBean(AdminIdentityResolver.class)
    public AdminIdentityResolver adminIdentityResolver(AuthenticatedAccountService accounts) {
        return request -> {
            if (request.getUserPrincipal() instanceof AdminPrincipal p) {
                if (p.accountId() == null || !"ADMIN".equalsIgnoreCase(p.role())) {
                    throw new AppException(ErrorCode.FORBIDDEN_ROLE);
                }
                return new AdminIdentityResolver.Identity(p.accountId(), p.email(), p.role());
            }
            return accounts.resolve(request.getHeader("Authorization"), "ADMIN")
                    .map(identity -> new AdminIdentityResolver.Identity(
                            identity.accountId(), identity.email(), identity.role()))
                    .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
        };
    }
}
