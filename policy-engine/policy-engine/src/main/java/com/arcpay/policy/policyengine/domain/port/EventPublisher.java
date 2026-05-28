package com.arcpay.policy.policyengine.domain.port;

public interface EventPublisher {

    void publish(Object event);
}
