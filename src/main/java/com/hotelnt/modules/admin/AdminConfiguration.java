package com.hotelnt.modules.admin;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
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
        @Tag(name = "admin-listing-controller")
})
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class AdminConfiguration {

    /**
     * Bo phan nhan dien Admin tam thoi trong khi cho module JWT cua Phu.
     * Khi Phu cung cap bean AdminIdentityResolver chinh thuc, Spring se tu dong
     * thay the va vo hieu hoa bean fallback nay nho @ConditionalOnMissingBean.
     */
    @Bean
    @ConditionalOnMissingBean(AdminIdentityResolver.class)
    public AdminIdentityResolver adminIdentityResolver() {
        return request -> {
            // Neu da duoc xac thuc qua Spring Security Principal
            if (request.getUserPrincipal() instanceof AdminPrincipal p) {
                if (p.accountId() == null || !"ADMIN".equalsIgnoreCase(p.role())) {
                    throw new AppException(ErrorCode.FORBIDDEN_ROLE);
                }
                return new AdminIdentityResolver.Identity(p.accountId(), p.email(), p.role());
            }

            // Kiem tra bat buoc phai co Header Authorization dang Bearer Token
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ") && auth.length() > 7) {
                return new AdminIdentityResolver.Identity(1, "admin@hotelnt.local", "ADMIN");
            }

            // Neu khong co Token -> tu choi truy cap ngay lap tuc
            throw new AppException(ErrorCode.TOKEN_INVALID);
        };
    }
}
