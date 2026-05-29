package com.arcpay.compliance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScreeningEnumTest {

    @Test
    void shouldExposeAllVerdictValues() {
        // given / when
        var values = Verdict.values();

        // then
        assertThat(values).containsExactly(Verdict.PASS, Verdict.HOLD, Verdict.BLOCK);
    }

    @Test
    void shouldExposeAllCheckTypeValues() {
        // given / when
        var values = CheckType.values();

        // then
        assertThat(values).containsExactly(
                CheckType.SANCTIONS_OFAC,
                CheckType.SANCTIONS_UN,
                CheckType.SANCTIONS_EU,
                CheckType.SANCTIONS_UK,
                CheckType.WATCHLIST,
                CheckType.ONCHAIN_INTERACTION,
                CheckType.ONCHAIN_NOVELTY,
                CheckType.ONCHAIN_MIXER);
    }

    @Test
    void shouldExposeAllCheckResultValues() {
        // given / when
        var values = CheckResult.values();

        // then
        assertThat(values).containsExactly(
                CheckResult.CLEAR,
                CheckResult.LOW_RISK,
                CheckResult.FLAGGED,
                CheckResult.MATCH);
    }

    @Test
    void shouldExposeAllReviewStateValues() {
        // given / when
        var values = ReviewState.values();

        // then
        assertThat(values).containsExactly(
                ReviewState.PENDING,
                ReviewState.APPROVED,
                ReviewState.REJECTED);
    }
}
