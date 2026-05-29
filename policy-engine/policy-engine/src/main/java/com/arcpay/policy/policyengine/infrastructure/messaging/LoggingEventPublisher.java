package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interim {@link EventPublisher} adapter that logs domain events instead of publishing them to
 * Kafka. The transactional outbox publisher (Spring Cloud Stream + namastack) is delivered in
 * issue #52, at which point this bean is replaced. It exists now so that command handlers wiring
 * an {@link EventPublisher} (issue #46) can be autowired and the application context starts.
 *
 * <p>The bean is registered through a {@link ConditionalOnMissingBean}-guarded factory method so
 * the real {@code OutboxEventPublisher} from issue #52 takes precedence without a duplicate-bean
 * conflict. ({@code @ConditionalOnMissingBean} is only reliable on {@code @Bean} factory methods,
 * not on component-scanned classes.) The {@link Propagation#MANDATORY} propagation on
 * {@link #publish(Object)} matches the namastack-outbox Invariant 1 contract: the publisher must
 * join the caller's transaction and fail fast if invoked outside one.
 */
@Slf4j
class LoggingEventPublisher implements EventPublisher {

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Object event) {
        log.info("Publishing domain event {}", event);
    }

    @Configuration
    static class LoggingEventPublisherConfiguration {

        @Bean
        @ConditionalOnMissingBean(EventPublisher.class)
        EventPublisher loggingEventPublisher() {
            return new LoggingEventPublisher();
        }
    }
}
