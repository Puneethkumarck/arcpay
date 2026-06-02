package com.arcpay.compliance.domain.event;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_DECISION_REASON;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_LIST_VERSION_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_REVIEWER_PRINCIPAL;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENED_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.arcpay.compliance.domain.model.Verdict;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScreeningEventTest {

    @Test
    void shouldExposeTopicConstants() {
        // given / when / then
        assertThat(ScreeningCompleted.TOPIC).isEqualTo("screening.completed");
        assertThat(ScreeningApproved.TOPIC).isEqualTo("screening.approved");
        assertThat(ScreeningRejected.TOPIC).isEqualTo("screening.rejected");
    }

    @Test
    void shouldBuildScreeningCompletedWithPayload() {
        // given / when
        var event = ScreeningCompleted.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .verdict(Verdict.PASS)
                .riskScore(0)
                .checks(List.of())
                .listVersionId(SOME_LIST_VERSION_ID)
                .screenedAt(SOME_SCREENED_AT)
                .build();

        // then
        var expected = new ScreeningCompleted(
                SOME_PAYMENT_ID, SOME_AGENT_ID, Verdict.PASS, 0, List.of(), SOME_LIST_VERSION_ID, SOME_SCREENED_AT);
        assertThat(event).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildScreeningApprovedWithPayload() {
        // given / when
        var event = ScreeningApproved.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reviewer(SOME_REVIEWER_PRINCIPAL)
                .reason(SOME_DECISION_REASON)
                .decidedAt(SOME_SCREENED_AT)
                .build();

        // then
        var expected =
                new ScreeningApproved(SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_DECISION_REASON, SOME_SCREENED_AT);
        assertThat(event).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildScreeningRejectedWithPayload() {
        // given / when
        var event = ScreeningRejected.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reviewer(SOME_REVIEWER_PRINCIPAL)
                .reason(SOME_DECISION_REASON)
                .decidedAt(SOME_SCREENED_AT)
                .build();

        // then
        var expected =
                new ScreeningRejected(SOME_PAYMENT_ID, SOME_REVIEWER_PRINCIPAL, SOME_DECISION_REASON, SOME_SCREENED_AT);
        assertThat(event).usingRecursiveComparison().isEqualTo(expected);
    }
}
