package com.arcpay.compliance.infrastructure.messaging;

import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ComplianceOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    ComplianceOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
