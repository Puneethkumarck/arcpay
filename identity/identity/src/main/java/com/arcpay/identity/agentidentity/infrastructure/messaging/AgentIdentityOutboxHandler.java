package com.arcpay.identity.agentidentity.infrastructure.messaging;

import com.arcpay.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class AgentIdentityOutboxHandler extends AbstractOutboxHandler {

    public AgentIdentityOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
