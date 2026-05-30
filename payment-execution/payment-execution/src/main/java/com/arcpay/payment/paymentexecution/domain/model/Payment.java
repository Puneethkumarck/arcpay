package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record Payment(
        UUID paymentId,
        UUID agentId,
        UUID ownerId,
        String idempotencyKey,
        String requestFingerprint,
        String recipientAddress,
        BigDecimal amount,
        String currency,
        String memo,
        PaymentStatus status,
        RejectionReason rejectionReason,
        FailureReason failureReason,
        String txHash,
        String onChainRef,
        UUID policyEvaluationId,
        Integer complianceRiskScore,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    public Payment {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public Payment withStatus(PaymentStatus toStatus, Instant transitionedAt) {
        Objects.requireNonNull(toStatus, "toStatus must not be null");
        Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");
        return toBuilder()
                .status(toStatus)
                .updatedAt(transitionedAt)
                .build();
    }

    public Payment reject(RejectionReason reason, Instant transitionedAt) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");
        return toBuilder()
                .status(PaymentStatus.REJECTED)
                .rejectionReason(reason)
                .updatedAt(transitionedAt)
                .build();
    }

    public Payment fail(FailureReason reason, Instant transitionedAt) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");
        return toBuilder()
                .status(PaymentStatus.FAILED)
                .failureReason(reason)
                .updatedAt(transitionedAt)
                .build();
    }

    public Payment complete(Instant transitionedAt) {
        Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");
        return toBuilder()
                .status(PaymentStatus.COMPLETED)
                .completedAt(transitionedAt)
                .updatedAt(transitionedAt)
                .build();
    }
}
