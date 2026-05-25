package com.arcpay.identity.agentidentity.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startable;

public final class TestContainerSupport {

    private TestContainerSupport() {}

    @SuppressWarnings("resource")
    public static PostgreSQLContainer<?> postgres(String databaseName) {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName(databaseName)
                .withUsername("test")
                .withPassword("test");
    }

    public static void startAll(Startable... containers) {
        for (var container : containers) {
            try {
                container.start();
            } catch (RuntimeException ex) {
                safeStop(containers);
                throw ex;
            }
        }
        registerShutdownHook(containers);
    }

    private static void safeStop(Startable... containers) {
        for (var container : containers) {
            try {
                if (container != null) {
                    container.stop();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void registerShutdownHook(Startable... containers) {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> safeStop(containers),
                "testcontainers-shutdown"
        ));
    }

    public static void registerPostgresProperties(DynamicPropertyRegistry registry,
                                                   PostgreSQLContainer<?> postgres) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
