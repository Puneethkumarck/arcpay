package com.arcpay.payment.paymentexecution;

import com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.test.BusinessTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged.TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

abstract class PaymentExecutionBusinessTest extends BusinessTest {

    protected static WireMockServer identityServer;
    protected static WireMockServer policyServer;
    protected static WireMockServer settlementServer;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected JsonMapper jsonMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @BeforeAll
    static void startUpstreamStubs() {
        identityServer = new WireMockServer(0);
        policyServer = new WireMockServer(0);
        settlementServer = new WireMockServer(0);
        identityServer.start();
        policyServer.start();
        settlementServer.start();
    }

    @AfterAll
    static void stopUpstreamStubs() {
        identityServer.stop();
        policyServer.stop();
        settlementServer.stop();
    }

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.identity-service.url", () -> "http://localhost:" + identityServer.port());
        registry.add("arcpay.policy-service.url", () -> "http://localhost:" + policyServer.port());
        registry.add("arcpay.settlement-service.url", () -> "http://localhost:" + settlementServer.port());
    }

    @BeforeEach
    void resetUpstreamsAndDatabase() {
        identityServer.resetAll();
        policyServer.resetAll();
        settlementServer.resetAll();
        cleanDatabase();
    }

    protected void awaitStatus(UUID paymentId, PaymentStatus status) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(loadStatus(paymentId)).isEqualTo(status));
    }

    protected Payment loadPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElse(null);
    }

    protected PaymentStatus loadStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId).map(Payment::status).orElse(null);
    }

    protected List<String> awaitStatusEvents(UUID paymentId, int expectedCount) {
        var statuses = new ArrayList<String>();
        try (var consumer = newStatusConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), PaymentStatusChanged.class);
                            if (paymentId.equals(event.paymentId())) {
                                statuses.add(event.status());
                            }
                        }
                        return statuses.size() >= expectedCount;
                    });
        }
        return statuses;
    }

    private KafkaConsumer<String, String> newStatusConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "status-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
