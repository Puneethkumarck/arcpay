package com.arcpay.settlement.infrastructure.messaging;

import com.arcpay.settlement.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SettlementOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    SettlementOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
