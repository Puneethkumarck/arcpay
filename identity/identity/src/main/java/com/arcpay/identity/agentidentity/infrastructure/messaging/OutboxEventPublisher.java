package com.arcpay.identity.agentidentity.infrastructure.messaging;

import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class OutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("agentId", "ownerId"));
    }
}
