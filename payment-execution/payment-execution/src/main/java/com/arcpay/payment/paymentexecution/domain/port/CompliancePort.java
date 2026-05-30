package com.arcpay.payment.paymentexecution.domain.port;

import com.arcpay.payment.paymentexecution.domain.model.Payment;

public interface CompliancePort {

    void publishScreeningRequest(Payment payment);
}
