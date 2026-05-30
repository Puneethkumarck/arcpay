package com.arcpay.payment.paymentexecution.infrastructure.db;

import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByAgentIdAndIdempotencyKey(UUID agentId, String idempotencyKey);

    Page<PaymentEntity> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<PaymentEntity> findByOwnerIdAndAgentId(UUID ownerId, UUID agentId, Pageable pageable);

    Page<PaymentEntity> findByOwnerIdAndAgentIdAndStatus(UUID ownerId, UUID agentId, PaymentStatus status, Pageable pageable);

    Page<PaymentEntity> findByOwnerIdAndStatus(UUID ownerId, PaymentStatus status, Pageable pageable);
}
