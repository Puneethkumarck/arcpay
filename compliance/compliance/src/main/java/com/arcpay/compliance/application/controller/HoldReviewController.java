package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.application.dto.ReviewDecisionRequest;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.exception.UnauthorizedException;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ReviewAuthorizer;
import com.arcpay.compliance.domain.service.HoldReviewService;
import com.arcpay.platform.api.OwnerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/compliance/holds")
@RequiredArgsConstructor
@Validated
public class HoldReviewController {

    private final HoldReviewService holdReviewService;
    private final ReviewAuthorizer reviewAuthorizer;
    private final HoldReviewStore holdReviewStore;

    @PostMapping("/{paymentId}/approve")
    public HoldReviewResponse approve(
            @PathVariable UUID paymentId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        var review = authorize(paymentId);
        log.info("Approve requested paymentId={} reviewer={}", paymentId, review.principal());
        return HoldReviewResponse.from(holdReviewService.approveHold(
                paymentId, review.principal(), review.role(), request.reason()));
    }

    @PostMapping("/{paymentId}/reject")
    public HoldReviewResponse reject(
            @PathVariable UUID paymentId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        var review = authorize(paymentId);
        log.info("Reject requested paymentId={} reviewer={}", paymentId, review.principal());
        return HoldReviewResponse.from(holdReviewService.rejectHold(
                paymentId, review.principal(), review.role(), request.reason()));
    }

    private Reviewer authorize(UUID paymentId) {
        var hold = holdReviewStore.findByPaymentId(paymentId)
                .orElseThrow(() -> new HoldNotFoundException(paymentId));
        var reviewer = currentReviewer();
        if (!reviewAuthorizer.canReview(reviewer.principal(), hold.agentId())) {
            throw new UnauthorizedException(reviewer.principal(), hold.agentId());
        }
        return reviewer;
    }

    private Reviewer currentReviewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OwnerPrincipal owner) {
            return new Reviewer(owner.email(), owner.authority());
        }
        return new Reviewer(authentication != null ? authentication.getName() : null, null);
    }

    private record Reviewer(String principal, String role) {}
}
