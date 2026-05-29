package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.application.dto.ReviewDecisionRequest;
import com.arcpay.compliance.domain.exception.UnauthorizedException;
import com.arcpay.compliance.domain.service.HoldReviewService;
import com.arcpay.platform.api.OwnerPrincipal;
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

    @PostMapping("/{paymentId}/approve")
    public HoldReviewResponse approve(
            @PathVariable UUID paymentId,
            @RequestBody ReviewDecisionRequest request) {
        var reviewer = currentReviewer();
        log.info("Approve requested paymentId={} reviewerRole={}", paymentId, reviewer.role());
        return HoldReviewResponse.from(holdReviewService.approveHold(
                paymentId, reviewer.principal(), reviewer.role(), request.reason()));
    }

    @PostMapping("/{paymentId}/reject")
    public HoldReviewResponse reject(
            @PathVariable UUID paymentId,
            @RequestBody ReviewDecisionRequest request) {
        var reviewer = currentReviewer();
        log.info("Reject requested paymentId={} reviewerRole={}", paymentId, reviewer.role());
        return HoldReviewResponse.from(holdReviewService.rejectHold(
                paymentId, reviewer.principal(), reviewer.role(), request.reason()));
    }

    private Reviewer currentReviewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OwnerPrincipal owner)) {
            throw new UnauthorizedException("anonymous", null);
        }
        return new Reviewer(owner.email(), owner.authority());
    }

    private record Reviewer(String principal, String role) {}
}
