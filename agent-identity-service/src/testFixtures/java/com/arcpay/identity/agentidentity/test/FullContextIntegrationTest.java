package com.arcpay.identity.agentidentity.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.arcpay.identity.agentidentity.test.TestContainerSupport.postgres;
import static com.arcpay.identity.agentidentity.test.TestContainerSupport.registerPostgresProperties;
import static com.arcpay.identity.agentidentity.test.TestContainerSupport.startAll;

@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class FullContextIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("arcpay_identity_test");

    static {
        startAll(POSTGRES);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
    }
}
