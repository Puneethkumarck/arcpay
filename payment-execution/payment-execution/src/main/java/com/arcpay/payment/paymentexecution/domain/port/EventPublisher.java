package com.arcpay.payment.paymentexecution.domain.port;

public interface EventPublisher {

    void publish(Object event);
}
