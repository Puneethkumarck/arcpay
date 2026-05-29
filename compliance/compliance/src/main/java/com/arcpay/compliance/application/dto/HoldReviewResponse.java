package com.arcpay.compliance.application.dto;

import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record HoldReviewResponse(
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

    public static HoldReviewResponse from(HoldReview review) {
        return HoldReviewResponse.builder()
                .reviewId(review.reviewId())
                .screeningId(review.screeningId())
                .paymentId(review.paymentId())
                .agentId(review.agentId())
                .state(review.state())
                .reviewerPrincipal(review.reviewerPrincipal())
                .reviewerRole(review.reviewerRole())
                .reason(review.reason())
                .createdAt(review.createdAt())
                .decidedAt(review.decidedAt())
                .build();
    }
}
