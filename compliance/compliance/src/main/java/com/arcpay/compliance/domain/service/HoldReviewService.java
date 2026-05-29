package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.compliance.domain.port.HoldReviewStore;
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

    @Transactional
    public HoldReview approveHold(UUID paymentId, String principal, String role, String reason) {
        var decided = load(paymentId).approve(principal, role, reason);
        holdReviewStore.update(decided);
        eventPublisher.publish(ScreeningApproved.builder()
                .paymentId(decided.paymentId())
                .reviewer(decided.reviewerPrincipal())
                .reason(decided.reason())
                .decidedAt(decided.decidedAt())
                .build());
        log.info("Approved hold paymentId={} reviewer={}", decided.paymentId(), decided.reviewerPrincipal());
        return decided;
    }

    @Transactional
    public HoldReview rejectHold(UUID paymentId, String principal, String role, String reason) {
        var decided = load(paymentId).reject(principal, role, reason);
        holdReviewStore.update(decided);
        eventPublisher.publish(ScreeningRejected.builder()
                .paymentId(decided.paymentId())
                .reviewer(decided.reviewerPrincipal())
                .reason(decided.reason())
                .decidedAt(decided.decidedAt())
                .build());
        log.info("Rejected hold paymentId={} reviewer={}", decided.paymentId(), decided.reviewerPrincipal());
        return decided;
    }

    private HoldReview load(UUID paymentId) {
        return holdReviewStore.findByPaymentId(paymentId)
                .orElseThrow(() -> new HoldNotFoundException(paymentId));
    }
}
