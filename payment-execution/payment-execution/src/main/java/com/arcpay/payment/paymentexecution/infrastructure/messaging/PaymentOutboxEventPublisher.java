package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher;
import io.namastack.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class PaymentOutboxEventPublisher extends AbstractOutboxEventPublisher implements EventPublisher {

    PaymentOutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
