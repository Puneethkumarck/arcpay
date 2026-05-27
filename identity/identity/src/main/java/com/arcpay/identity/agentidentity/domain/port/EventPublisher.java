package com.arcpay.identity.agentidentity.domain.port;

public interface EventPublisher {

    void publish(Object event);
}
