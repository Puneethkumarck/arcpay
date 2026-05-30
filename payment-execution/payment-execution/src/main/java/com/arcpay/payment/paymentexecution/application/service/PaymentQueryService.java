package com.arcpay.payment.paymentexecution.application.service;

import com.arcpay.payment.paymentexecution.domain.exception.PaymentAccessDeniedException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public Payment getPayment(UUID paymentId, UUID ownerId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.ownerId().equals(ownerId)) {
            throw new PaymentAccessDeniedException(paymentId, ownerId);
        }
        return payment;
    }

    public Page<Payment> listPayments(UUID ownerId, UUID agentId, PaymentStatus status, Pageable pageable) {
        if (agentId != null && status != null) {
            return paymentRepository.findByOwnerIdAndAgentIdAndStatus(ownerId, agentId, status, pageable);
        }
        if (agentId != null) {
            return paymentRepository.findByOwnerIdAndAgentId(ownerId, agentId, pageable);
        }
        if (status != null) {
            return paymentRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        }
        return paymentRepository.findByOwnerId(ownerId, pageable);
    }
}
