package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class PaymentOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    PaymentOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
