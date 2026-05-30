package com.arcpay.payment.paymentexecution.domain.port;

public interface EventPublisher<T> {

    void publish(T event);
}
