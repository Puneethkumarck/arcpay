package com.arcpay.compliance.domain.model;

import com.arcpay.compliance.domain.exception.HoldAlreadyDecidedException;
import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_DECISION_REASON;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_REVIEWER_PRINCIPAL;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_REVIEWER_ROLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldReviewTest {

    @Test
    void shouldApproveHoldReviewAndUpdateState() {
        // given
        var review = SOME_HOLD_REVIEW_PENDING;

        // when
        var result = review.approve(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // then
        var expected = review.toBuilder()
                .state(ReviewState.APPROVED)
                .reviewerPrincipal(SOME_REVIEWER_PRINCIPAL)
                .reviewerRole(SOME_REVIEWER_ROLE)
                .reason(SOME_DECISION_REASON)
                .decidedAt(result.decidedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        assertThat(result.decidedAt()).isNotNull();
    }

    @Test
    void shouldRejectHoldReviewAndUpdateState() {
        // given
        var review = SOME_HOLD_REVIEW_PENDING;

        // when
        var result = review.reject(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // then
        var expected = review.toBuilder()
                .state(ReviewState.REJECTED)
                .reviewerPrincipal(SOME_REVIEWER_PRINCIPAL)
                .reviewerRole(SOME_REVIEWER_ROLE)
                .reason(SOME_DECISION_REASON)
                .decidedAt(result.decidedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        assertThat(result.decidedAt()).isNotNull();
    }

    @Test
    void shouldLeaveOriginalUnchangedAfterApproval() {
        // given
        var review = SOME_HOLD_REVIEW_PENDING;

        // when
        var result = review.approve(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // then
        assertThat(result).isNotSameAs(review);
        assertThat(review).usingRecursiveComparison().isEqualTo(SOME_HOLD_REVIEW_PENDING);
    }

    @Test
    void shouldRejectDecisionWhenReasonTooShort() {
        // given
        var review = SOME_HOLD_REVIEW_PENDING;

        // when / then
        assertThatThrownBy(() -> review.approve(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
    }

    @Test
    void shouldRejectNullPrincipalOnDecision() {
        // given
        var review = SOME_HOLD_REVIEW_PENDING;

        // when / then
        assertThatThrownBy(() -> review.approve(null, SOME_REVIEWER_ROLE, SOME_DECISION_REASON))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("principal");
    }

    @Test
    void shouldThrowWhenDecidingAlreadyDecidedReview() {
        // given
        var decided = SOME_HOLD_REVIEW_PENDING.approve(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON);

        // when / then
        assertThatThrownBy(() -> decided.reject(SOME_REVIEWER_PRINCIPAL, SOME_REVIEWER_ROLE, SOME_DECISION_REASON))
                .isInstanceOf(HoldAlreadyDecidedException.class)
                .hasMessageContaining(ReviewState.APPROVED.name());
    }

    @Test
    void shouldRejectNullRequiredFieldOnConstruction() {
        // given
        var builder = SOME_HOLD_REVIEW_PENDING.toBuilder().screeningId(null);

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("screeningId");
    }
}
