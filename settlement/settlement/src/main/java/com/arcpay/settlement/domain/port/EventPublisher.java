package com.arcpay.settlement.domain.port;

public interface EventPublisher {

    void publish(Object event);
}
