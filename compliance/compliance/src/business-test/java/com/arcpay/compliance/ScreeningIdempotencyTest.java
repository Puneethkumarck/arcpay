package com.arcpay.compliance;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.test.BusinessTest;
import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
class ScreeningIdempotencyTest extends BusinessTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @BeforeEach
    void resetState() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldRepublishExistingVerdictWithoutReScreeningOnDuplicate() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        var request = requestFor(paymentId, SOME_RECIPIENT_ADDRESS);

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(), request);
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                .until(() -> screeningResultCount(paymentId) == 1);
        var screenedAtAfterFirst = screenedAt(paymentId);
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(), request);

        // then
        var events = awaitCompletedEvents(paymentId, 2);
        assertThat(screeningResultCount(paymentId)).isEqualTo(1);
        assertThat(screenedAt(paymentId)).isEqualTo(screenedAtAfterFirst);
        assertThat(events).hasSize(2);
        assertThat(events.getLast()).usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(events.getFirst());
    }

    private PaymentScreeningRequested requestFor(UUID paymentId, String recipientAddress) {
        return PaymentScreeningRequested.builder()
                .paymentId(paymentId)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(recipientAddress)
                .amount(new BigDecimal("100.00"))
                .currency("USDC")
                .requestedAt(Instant.now())
                .build();
    }

    private List<ScreeningCompleted> awaitCompletedEvents(UUID paymentId, int expected) {
        try (var consumer = newCompletedConsumer()) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            var captured = new ArrayList<ScreeningCompleted>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId)) {
                                captured.add(event);
                            }
                        }
                        return captured.size() >= expected;
                    });
            return captured;
        }
    }

    private KafkaConsumer<String, String> newCompletedConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "idempotency-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private int screeningResultCount(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM screening_result WHERE payment_id = ?", Integer.class, paymentId);
    }

    private Instant screenedAt(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "SELECT screened_at FROM screening_result WHERE payment_id = ?", Instant.class, paymentId);
    }

    private void awaitSanctionsLoaded(UUID versionId) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> versionId.equals(sanctionsSetProvider.getCurrentSanctionsSet().versionId()));
    }

    private UUID seedSanctions(String address) {
        var versionId = UuidCreator.getTimeOrderedEpoch();
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, address, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
        return versionId;
    }
}
