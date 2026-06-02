package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import io.namastack.outbox.Outbox;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("agentId"));
    }
}
