package com.arcpay.payment.paymentexecution.domain.port;

import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    Optional<Payment> findByAgentIdAndIdempotencyKey(UUID agentId, String idempotencyKey);

    Page<Payment> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Payment> findByOwnerIdAndAgentId(UUID ownerId, UUID agentId, Pageable pageable);

    Page<Payment> findByOwnerIdAndAgentIdAndStatus(UUID ownerId, UUID agentId, PaymentStatus status, Pageable pageable);

    Page<Payment> findByOwnerIdAndStatus(UUID ownerId, PaymentStatus status, Pageable pageable);
}
