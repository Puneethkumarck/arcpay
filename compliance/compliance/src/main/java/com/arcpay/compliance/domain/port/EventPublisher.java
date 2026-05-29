package com.arcpay.compliance.domain.port;

public interface EventPublisher {

    void publish(Object event);
}
