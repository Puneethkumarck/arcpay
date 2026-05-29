package com.arcpay.compliance.application.stream;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ScreeningEngine;
import com.arcpay.compliance.domain.port.ScreeningStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_HOLD;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_PASS;
import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ScreeningRequestHandlerTest {

    private static final PaymentScreeningRequested SOME_REQUEST = PaymentScreeningRequested.builder()
            .paymentId(SOME_PAYMENT_ID)
            .agentId(SOME_AGENT_ID)
            .recipientAddress(SOME_RECIPIENT_ADDRESS)
            .amount(new BigDecimal("100.00"))
            .currency("USDC")
            .requestedAt(Instant.parse("2026-06-01T10:00:00Z"))
            .build();

    @Mock
    private ScreeningEngine screeningEngine;

    @Mock
    private ScreeningStore screeningStore;

    @Mock
    private HoldReviewStore holdReviewStore;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ScreeningRequestHandler handler;

    @Test
    void shouldReturnExistingResultOnDuplicatePaymentId() {
        // given
        given(screeningStore.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(SOME_SCREENING_RESULT_PASS));

        // when
        handler.handle(SOME_REQUEST);

        // then
        then(screeningEngine).should(never()).screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);
        then(screeningStore).should(never()).insert(SOME_SCREENING_RESULT_PASS, SOME_SCREENING_RESULT_PASS.checks());
        then(eventPublisher).should().publish(eqIgnoringTimestamps(completedFrom(SOME_SCREENING_RESULT_PASS)));
    }

    @Test
    void shouldScreenAndPersistNewResult() {
        // given
        given(screeningStore.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS))
                .willReturn(SOME_SCREENING_RESULT_PASS);

        // when
        handler.handle(SOME_REQUEST);

        // then
        then(screeningStore).should().insert(SOME_SCREENING_RESULT_PASS, SOME_SCREENING_RESULT_PASS.checks());
        verifyNoInteractions(holdReviewStore);
        then(eventPublisher).should().publish(eqIgnoringTimestamps(completedFrom(SOME_SCREENING_RESULT_PASS)));
    }

    @Test
    void shouldInsertHoldReviewOnHoldVerdict() {
        // given
        given(screeningStore.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS))
                .willReturn(SOME_SCREENING_RESULT_HOLD);

        // when
        handler.handle(SOME_REQUEST);

        // then
        then(holdReviewStore).should().insert(eqIgnoring(expectedPendingReview(), "reviewId", "createdAt"));
        then(eventPublisher).should().publish(eqIgnoringTimestamps(completedFrom(SOME_SCREENING_RESULT_HOLD)));
    }

    private static HoldReview expectedPendingReview() {
        return HoldReview.builder()
                .screeningId(SOME_SCREENING_RESULT_HOLD.screeningId())
                .paymentId(SOME_SCREENING_RESULT_HOLD.paymentId())
                .agentId(SOME_SCREENING_RESULT_HOLD.agentId())
                .state(ReviewState.PENDING)
                .build();
    }

    private static ScreeningCompleted completedFrom(com.arcpay.compliance.domain.model.ScreeningResult result) {
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
