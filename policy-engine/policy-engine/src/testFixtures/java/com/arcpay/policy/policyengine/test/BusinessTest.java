package com.arcpay.policy.policyengine.test;

import com.github.tomakehurst.wiremock.WireMockServer;
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

    protected static final String SERVICE_TOKEN = "test-service-token";

    static final PostgreSQLContainer<?> POSTGRES = postgres("arcpay_policy_biz");
    static final KafkaContainer KAFKA = kafka();
    static final WireMockServer IDENTITY_SERVICE = new WireMockServer(0);

    static {
        startAll(POSTGRES, KAFKA);
        IDENTITY_SERVICE.start();
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
        registry.add("arcpay.security.service-token", () -> SERVICE_TOKEN);
        registry.add("arcpay.identity-service.url", () -> "http://localhost:" + IDENTITY_SERVICE.port());
    }

    protected RestClient restClient() {
        if (cachedRestClient == null) {
            cachedRestClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return cachedRestClient;
    }

    protected WireMockServer identityService() {
        return IDENTITY_SERVICE;
    }

    protected void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM policyengine_outbox_record");
        jdbcTemplate.update("DELETE FROM policy_evaluations");
        jdbcTemplate.update("DELETE FROM spending_ledger");
        jdbcTemplate.update("DELETE FROM policies");
        jdbcTemplate.update("DELETE FROM spending_locks");
    }
}
