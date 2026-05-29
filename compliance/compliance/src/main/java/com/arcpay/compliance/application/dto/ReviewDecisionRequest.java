package com.arcpay.compliance.application.dto;

import com.arcpay.compliance.domain.exception.ReviewReasonInvalidException;
import jakarta.validation.constraints.NotBlank;

public record ReviewDecisionRequest(
        @NotBlank(message = "Review reason missing or < 10 characters")
        String reason
) {

    private static final int MINIMUM_REASON_LENGTH = 10;

    public ReviewDecisionRequest {
        if (reason != null && reason.strip().length() < MINIMUM_REASON_LENGTH) {
            throw new ReviewReasonInvalidException("Review reason missing or < 10 characters");
        }
    }
}
