package com.arcpay.compliance.infrastructure.messaging;

import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ComplianceOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    ComplianceOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
