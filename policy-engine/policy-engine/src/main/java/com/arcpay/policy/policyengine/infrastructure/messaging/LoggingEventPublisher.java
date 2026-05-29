package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Interim {@link EventPublisher} adapter that logs domain events instead of publishing them to
 * Kafka. The transactional outbox publisher (Spring Cloud Stream + namastack) is delivered in
 * issue #52, at which point this bean is replaced. It exists now so that command handlers wiring
 * an {@link EventPublisher} (issue #46) can be autowired and the application context starts.
 */
@Slf4j
@Component
class LoggingEventPublisher implements EventPublisher {

    @Override
    public void publish(Object event) {
        log.info("Publishing domain event {}", event);
    }
}
