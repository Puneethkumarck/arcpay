package com.arcpay.compliance.application.stream;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.OnChainFixtures.blockNumberJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.emptyLogsJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ScreeningRequestedListenerDltIntegrationTest extends FullContextIntegrationTest {

    private static final String DLT_TOPIC = PaymentScreeningRequested.TOPIC + ".dlt";
    private static final String MALFORMED_ADDRESS = "definitely-not-an-evm-address";
    private static final String POISON_PAYLOAD = "{not-valid-json";

    private static WireMockServer rpcServer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @BeforeAll
    static void startRpcStub() {
        rpcServer = new WireMockServer(0);
        rpcServer.start();
    }

    @AfterAll
    static void stopRpcStub() {
        if (rpcServer != null) {
            rpcServer.stop();
        }
    }

    @DynamicPropertySource
    static void onChainProperties(DynamicPropertyRegistry registry) {
        registry.add("compliance.onchain.rpc-url", () -> "http://localhost:" + rpcServer.port());
    }

    @BeforeEach
    void resetState() {
        rpcServer.resetAll();
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_blockNumber")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(blockNumberJson(100000))));
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(emptyLogsJson())));
    }

    @Test
    void shouldRouteMalformedAddressFailureToDeadLetterTopic() {
        // given
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        var before = dltCount();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, MALFORMED_ADDRESS));

        // then
        var dltRecord = awaitDeadLetterRecord(paymentId.toString());
        assertThat(dltRecord.value()).contains(paymentId.toString()).contains(MALFORMED_ADDRESS);
        assertThat(headerValue(dltRecord, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(PaymentScreeningRequested.TOPIC);
        assertThat(headerValue(dltRecord, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN)).contains("MalformedAddressException");
        assertThat(dltCount()).isGreaterThan(before);
    }

    @Test
    void shouldRoutePoisonMessageToDeadLetterTopic() {
        // given
        var key = UuidCreator.getTimeOrderedEpoch().toString();
        var before = dltCount();

        // when
        try (var producer = rawProducer()) {
            producer.send(new ProducerRecord<>(PaymentScreeningRequested.TOPIC, key, POISON_PAYLOAD));
            producer.flush();
        }

        // then
        var dltRecord = awaitDeadLetterRecord(key);
        assertThat(dltRecord.value()).isEqualTo(POISON_PAYLOAD);
        assertThat(headerValue(dltRecord, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(PaymentScreeningRequested.TOPIC);
        assertThat(headerValue(dltRecord, KafkaHeaders.DLT_EXCEPTION_FQCN))
                .isEqualTo(DeserializationException.class.getName());
        assertThat(dltCount()).isGreaterThan(before);
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord(String key) {
        try (var consumer = newDltConsumer()) {
            consumer.subscribe(List.of(DLT_TOPIC));
            var found = new AtomicReference<ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(2))) {
                            if (key.equals(record.key())) {
                                found.set(record);
                                return true;
                            }
                        }
                        return false;
                    });
            return found.get();
        }
    }

    private String headerValue(ConsumerRecord<String, String> record, String header) {
        var value = record.headers().lastHeader(header);
        return value == null ? null : new String(value.value(), StandardCharsets.UTF_8);
    }

    private double dltCount() {
        var counter = meterRegistry.find("compliance.screening.dlt").counter();
        return counter == null ? 0.0 : counter.count();
    }

    private PaymentScreeningRequested requestFor(UUID paymentId, String recipientAddress) {
        return PaymentScreeningRequested.builder()
                .paymentId(paymentId)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(recipientAddress)
                .amount(new BigDecimal("100.00"))
                .currency("USDC")
                .requestedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
    }

    private KafkaConsumer<String, String> newDltConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private KafkaProducer<String, String> rawProducer() {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }
}
