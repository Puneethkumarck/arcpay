package com.arcpay.settlement.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import com.arcpay.settlement.domain.port.EventPublisher;
import io.namastack.outbox.Outbox;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class SettlementOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    SettlementOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
