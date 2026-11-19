package com.hotelnt.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.liquibase.contexts=dev",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.auth.jwt-secret=01234567890123456789012345678901"
})
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    public static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();
}
