package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.event.PolicyViolationDetected;
import com.arcpay.platform.infrastructure.messaging.AbstractOutboxHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class PolicyEngineOutboxHandler extends AbstractOutboxHandler {

    static final Map<Class<?>, String> TOPIC_MAP = Map.of(
            PolicyCreated.class, PolicyCreated.TOPIC,
            PolicyViolationDetected.class, PolicyViolationDetected.TOPIC
    );

    public PolicyEngineOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
