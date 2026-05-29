package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class OutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("agentId"));
    }
}
