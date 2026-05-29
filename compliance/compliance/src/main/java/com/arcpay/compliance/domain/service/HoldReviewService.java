package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.exception.UnauthorizedException;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ReviewAuthorizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldReviewService {

    private final HoldReviewStore holdReviewStore;
    private final EventPublisher eventPublisher;
    private final ReviewAuthorizer reviewAuthorizer;

    @Transactional
    public HoldReview approveHold(UUID paymentId, String principal, String role, String reason) {
        var hold = authorize(load(paymentId), principal, role);
        var decided = hold.approve(principal, role, reason);
        holdReviewStore.update(decided);
        eventPublisher.publish(ScreeningApproved.builder()
                .paymentId(decided.paymentId())
                .reviewer(decided.reviewerPrincipal())
                .reason(decided.reason())
                .decidedAt(decided.decidedAt())
                .build());
        log.info("Approved hold paymentId={} reviewerRole={}", decided.paymentId(), decided.reviewerRole());
        return decided;
    }

    @Transactional
    public HoldReview rejectHold(UUID paymentId, String principal, String role, String reason) {
        var hold = authorize(load(paymentId), principal, role);
        var decided = hold.reject(principal, role, reason);
        holdReviewStore.update(decided);
        eventPublisher.publish(ScreeningRejected.builder()
                .paymentId(decided.paymentId())
                .reviewer(decided.reviewerPrincipal())
                .reason(decided.reason())
                .decidedAt(decided.decidedAt())
                .build());
        log.info("Rejected hold paymentId={} reviewerRole={}", decided.paymentId(), decided.reviewerRole());
        return decided;
    }

    private HoldReview authorize(HoldReview hold, String principal, String role) {
        if (!reviewAuthorizer.canReview(principal, role, hold.agentId())) {
            throw new UnauthorizedException(principal, hold.agentId());
        }
        return hold;
    }

    private HoldReview load(UUID paymentId) {
        return holdReviewStore.findByPaymentId(paymentId)
                .orElseThrow(() -> new HoldNotFoundException(paymentId));
    }
}
