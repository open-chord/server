package com.openchord.server.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Owns the PostgreSQL container shared by the backend integration-test suite. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("openchord")
                .withUsername("openchord")
                .withPassword("openchord");
    }
}
