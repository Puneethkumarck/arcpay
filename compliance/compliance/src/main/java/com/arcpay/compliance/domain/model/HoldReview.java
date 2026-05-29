package com.arcpay.compliance.domain.model;

import com.arcpay.compliance.domain.exception.HoldAlreadyDecidedException;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record HoldReview(
        UUID reviewId,
        UUID screeningId,
        UUID paymentId,
        UUID agentId,
        ReviewState state,
        String reviewerPrincipal,
        String reviewerRole,
        String reason,
        Instant createdAt,
        Instant decidedAt
) {

    private static final int MINIMUM_REASON_LENGTH = 10;

    public HoldReview {
        Objects.requireNonNull(screeningId, "screeningId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }

    public HoldReview approve(String principal, String role, String reason) {
        return decide(ReviewState.APPROVED, principal, role, reason);
    }

    public HoldReview reject(String principal, String role, String reason) {
        return decide(ReviewState.REJECTED, principal, role, reason);
    }

    private HoldReview decide(ReviewState target, String principal, String role, String reason) {
        if (state != ReviewState.PENDING) {
            throw new HoldAlreadyDecidedException(paymentId, state);
        }
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.strip().length() < MINIMUM_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "reason must be at least " + MINIMUM_REASON_LENGTH + " characters");
        }
        return toBuilder()
                .state(target)
                .reviewerPrincipal(principal)
                .reviewerRole(role)
                .reason(reason)
                .decidedAt(Instant.now())
                .build();
    }
}
