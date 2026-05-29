package com.arcpay.compliance.domain.port;

public interface EventPublisher<T> {

    void publish(T event);
}
