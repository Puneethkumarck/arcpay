package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.exception.HoldAlreadyDecidedException;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.port.EventPublisher;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_DECISION_REASON;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_REVIEWER_PRINCIPAL;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_REVIEWER_ROLE;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HoldReviewServiceTest {

    @Mock
    private HoldReviewStore holdReviewStore;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private HoldReviewService service;

    @Test
    void shouldApproveHoldAndReturnApprovedState() {
        // given
        given(holdReviewStore.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(SOME_HOLD_REVIEW_PENDING));

        // when
        var actual = service.approveHold(
                SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // then
        assertThat(actual).usingRecursiveComparison().ignoringFields("decidedAt").isEqualTo(expectedDecided(ReviewState.APPROVED));
        then(holdReviewStore).should().update(eqIgnoringTimestamps(expectedDecided(ReviewState.APPROVED)));
        then(eventPublisher).should().publish(eqIgnoringTimestamps(ScreeningApproved.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reviewer(SOME_REVIEWER_PRINCIPAL)
                .reason(SOME_DECISION_REASON)
                .decidedAt(actual.decidedAt())
                .build()));
    }

    @Test
    void shouldRejectHoldAndReturnRejectedState() {
        // given
        given(holdReviewStore.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(SOME_HOLD_REVIEW_PENDING));

        // when
        var actual = service.rejectHold(
                SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // then
        assertThat(actual).usingRecursiveComparison().ignoringFields("decidedAt").isEqualTo(expectedDecided(ReviewState.REJECTED));
        then(holdReviewStore).should().update(eqIgnoringTimestamps(expectedDecided(ReviewState.REJECTED)));
        then(eventPublisher).should().publish(eqIgnoringTimestamps(ScreeningRejected.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reviewer(SOME_REVIEWER_PRINCIPAL)
                .reason(SOME_DECISION_REASON)
                .decidedAt(actual.decidedAt())
                .build()));
    }

    @Test
    void shouldThrowHoldAlreadyDecidedExceptionIfStateNotPending() {
        // given
        var alreadyApproved = SOME_HOLD_REVIEW_PENDING.toBuilder().state(ReviewState.APPROVED).build();
        given(holdReviewStore.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(alreadyApproved));

        // when / then
        assertThatThrownBy(() -> service.approveHold(
                SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON))
                .isInstanceOf(HoldAlreadyDecidedException.class);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowHoldNotFoundExceptionIfPaymentIdAbsent() {
        // given
        given(holdReviewStore.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.approveHold(
                SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON))
                .isInstanceOf(HoldNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    private static HoldReview expectedDecided(ReviewState state) {
        return SOME_HOLD_REVIEW_PENDING.toBuilder()
                .agentId(SOME_AGENT_ID)
                .state(state)
                .reviewerPrincipal(SOME_REVIEWER_PRINCIPAL)
                .reviewerRole(SOME_REVIEWER_ROLE)
                .reason(SOME_DECISION_REASON)
                .build();
    }
}
