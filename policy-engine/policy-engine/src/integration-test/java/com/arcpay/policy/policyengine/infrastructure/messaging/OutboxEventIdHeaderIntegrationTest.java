package com.arcpay.policy.policyengine.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.arcpay.platform.infrastructure.messaging.OutboxHeaders;
import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxEventIdHeaderIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void shouldDeliverEventToKafkaWithNonBlankEventIdHeader() {
        // given
        var agentId = UUID.randomUUID();
        var event = new PolicyCreated(UUID.randomUUID(), agentId, UUID.randomUUID(), 1, "0xabc", Instant.now());

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        // then
        var record = awaitRecord(agentId.toString());
        var header = record.headers().lastHeader(OutboxHeaders.EVENT_ID_HEADER);
        assertThat(header).isNotNull();
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isNotBlank();
    }

    private ConsumerRecord<String, String> awaitRecord(String key) {
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of(PolicyCreated.TOPIC));
            var found = new AtomicReference<ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofMillis(500))
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

    private KafkaConsumer<String, String> newConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "event-id-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
