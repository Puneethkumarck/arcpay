package com.arcpay.identity.agentidentity.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
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
@DirtiesContext
public abstract class BusinessTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("arcpay_identity_biz");
    static final KafkaContainer KAFKA = kafka();

    static {
        startAll(POSTGRES, KAFKA);
    }

    @LocalServerPort
    private int port;

    private RestClient cachedRestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
        registry.add("arcpay.security.service-token", () -> "test-service-token");
    }

    protected RestClient restClient() {
        if (cachedRestClient == null) {
            cachedRestClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return cachedRestClient;
    }

    protected void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM agentidentity_outbox_record");
        jdbcTemplate.update("DELETE FROM gas_usage");
        jdbcTemplate.update("DELETE FROM idempotency_keys");
        jdbcTemplate.update("DELETE FROM agents");
        jdbcTemplate.update("DELETE FROM owners");
    }
}
