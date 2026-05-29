package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class PolicyEngineOutboxHandler extends AbstractOutboxHandler {

    public PolicyEngineOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
