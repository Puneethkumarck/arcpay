package com.arcpay.compliance.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class ComplianceOutboxHandler extends AbstractOutboxHandler {

    ComplianceOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
