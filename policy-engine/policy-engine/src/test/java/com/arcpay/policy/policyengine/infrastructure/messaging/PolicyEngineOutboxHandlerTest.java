package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.event.PolicyViolationDetected;
import io.namastack.outbox.handler.OutboxRecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PolicyEngineOutboxHandlerTest {

    private static final String KEY = "11111111-2222-3333-4444-555555555555";
    private static final UUID AGENT_ID = UUID.fromString(KEY);

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxRecordMetadata metadata;

    @Test
    void shouldPublishPolicyCreatedToPolicyCreatedTopic() {
        // given
        var handler = new PolicyEngineOutboxHandler(kafkaTemplate);
        var event = new PolicyCreated(
                UUID.randomUUID(),
                AGENT_ID,
                UUID.randomUUID(),
                1,
                "0xabc",
                Instant.parse("2026-05-29T10:00:00Z"));
        given(metadata.getKey()).willReturn(KEY);
        given(kafkaTemplate.send("policy.created", KEY, event))
                .willReturn(CompletableFuture.completedFuture(null));

        // when
        handler.handle(event, metadata);

        // then
        then(kafkaTemplate).should().send("policy.created", KEY, event);
    }

    @Test
    void shouldPublishPolicyViolationDetectedToViolationTopic() {
        // given
        var handler = new PolicyEngineOutboxHandler(kafkaTemplate);
        var event = new PolicyViolationDetected(
                UUID.randomUUID(),
                AGENT_ID,
                UUID.randomUUID(),
                "DAILY_LIMIT",
                "Daily spending limit exceeded",
                new BigDecimal("100.000000"),
                Instant.parse("2026-05-29T10:00:00Z"));
        given(metadata.getKey()).willReturn(KEY);
        given(kafkaTemplate.send("policy.violation-detected", KEY, event))
                .willReturn(CompletableFuture.completedFuture(null));

        // when
        handler.handle(event, metadata);

        // then
        then(kafkaTemplate).should().send("policy.violation-detected", KEY, event);
    }

    @Test
    void shouldMapEventTypesToTheirTopics() {
        // given
        var expected = java.util.Map.of(
                PolicyCreated.class, "policy.created",
                PolicyViolationDetected.class, "policy.violation-detected");

        // when
        var actual = PolicyEngineOutboxHandler.TOPIC_MAP;

        // then
        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }
}
