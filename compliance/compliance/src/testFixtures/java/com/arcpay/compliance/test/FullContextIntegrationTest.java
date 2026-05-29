package com.arcpay.compliance.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import static com.arcpay.platform.test.TestContainerSupport.kafka;
import static com.arcpay.platform.test.TestContainerSupport.postgres;
import static com.arcpay.platform.test.TestContainerSupport.registerKafkaProperties;
import static com.arcpay.platform.test.TestContainerSupport.registerPostgresProperties;
import static com.arcpay.platform.test.TestContainerSupport.startAll;

@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class FullContextIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("arcpay_compliance_test");
    static final KafkaContainer KAFKA = kafka();

    static {
        startAll(POSTGRES, KAFKA);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
    }
}
