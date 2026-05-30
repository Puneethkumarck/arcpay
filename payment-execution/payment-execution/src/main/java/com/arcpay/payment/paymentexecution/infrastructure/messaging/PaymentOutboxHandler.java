package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class PaymentOutboxHandler extends AbstractOutboxHandler {

    PaymentOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
