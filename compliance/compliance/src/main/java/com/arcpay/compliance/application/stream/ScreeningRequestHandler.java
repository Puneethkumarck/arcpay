package com.arcpay.compliance.application.stream;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ScreeningEngine;
import com.arcpay.compliance.domain.port.ScreeningStore;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
class ScreeningRequestHandler {

    private final ScreeningEngine screeningEngine;
    private final ScreeningStore screeningStore;
    private final HoldReviewStore holdReviewStore;
    private final EventPublisher eventPublisher;

    @Transactional
    void handle(PaymentScreeningRequested event) {
        var existing = screeningStore.findByPaymentId(event.paymentId());
        if (existing.isPresent()) {
            log.info("Existing screening result for paymentId={}, re-publishing verdict", event.paymentId());
            eventPublisher.publish(toCompleted(existing.get()));
            return;
        }

        var result = screeningEngine.screen(event.paymentId(), event.agentId(), event.recipientAddress());
        screeningStore.insert(result, result.checks());

        if (result.verdict() == Verdict.HOLD) {
            holdReviewStore.insert(pendingReview(result));
        }

        eventPublisher.publish(toCompleted(result));
        log.info("Screened paymentId={} verdict={} riskScore={}",
                result.paymentId(), result.verdict(), result.riskScore());
    }

    private static HoldReview pendingReview(ScreeningResult result) {
        return HoldReview.builder()
                .reviewId(UuidCreator.getTimeOrderedEpoch())
                .screeningId(result.screeningId())
                .paymentId(result.paymentId())
                .agentId(result.agentId())
                .state(ReviewState.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    private static ScreeningCompleted toCompleted(ScreeningResult result) {
        return ScreeningCompleted.builder()
                .paymentId(result.paymentId())
                .agentId(result.agentId())
                .verdict(result.verdict())
                .riskScore(result.riskScore())
                .checks(result.checks())
                .listVersionId(result.listVersionId())
                .screenedAt(result.screenedAt())
                .build();
    }
}
