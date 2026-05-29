package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.exception.MalformedAddressException;
import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.model.ScreeningThreshold;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_LIST_VERSION_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS_CHECKSUMMED;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONS_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    private static final ScreeningThreshold HOLD_THRESHOLD =
            ScreeningThreshold.builder().holdThreshold(50).build();

    private static final ScreeningCheck WATCHLIST_HIT = ScreeningCheck.builder()
            .type(CheckType.WATCHLIST)
            .result(CheckResult.MATCH)
            .matchScore(100)
            .details(Map.of("matched", Boolean.TRUE))
            .build();

    private static final ScreeningCheck NOVELTY_HIT = ScreeningCheck.builder()
            .type(CheckType.ONCHAIN_NOVELTY)
            .result(CheckResult.LOW_RISK)
            .matchScore(10)
            .details(Map.of("transferCount", 0))
            .build();

    private static final ScreeningCheck INTERACTION_HIT = ScreeningCheck.builder()
            .type(CheckType.ONCHAIN_INTERACTION)
            .result(CheckResult.FLAGGED)
            .matchScore(70)
            .details(Map.of("counterparty", SOME_SANCTIONED_ADDRESS))
            .build();

    private static final ScreeningCheck CLEAR_WATCHLIST = ScreeningCheck.builder()
            .type(CheckType.WATCHLIST)
            .result(CheckResult.CLEAR)
            .matchScore(0)
            .details(Map.of("matched", Boolean.FALSE))
            .build();

    private static final ScreeningCheck SCORE_49 = ScreeningCheck.builder()
            .type(CheckType.ONCHAIN_INTERACTION)
            .result(CheckResult.FLAGGED)
            .matchScore(49)
            .details(Map.of())
            .build();

    private static final ScreeningCheck SCORE_50 = ScreeningCheck.builder()
            .type(CheckType.ONCHAIN_INTERACTION)
            .result(CheckResult.FLAGGED)
            .matchScore(50)
            .details(Map.of())
            .build();

    @Mock
    private SanctionsSetProvider sanctionsSetProvider;

    @Mock
    private RiskSignalProvider watchlistSignalProvider;

    @Mock
    private RiskSignalProvider onChainSignalProvider;

    @Test
    void shouldBlockWhenRecipientIsSanctioned() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        var service = new ScreeningService(sanctionsSetProvider, List.of(watchlistSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_SANCTIONED_ADDRESS);

        // then
        var expected = ScreeningResult.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_SANCTIONED_ADDRESS)
                .verdict(Verdict.BLOCK)
                .riskScore(100)
                .checks(List.of(ScreeningCheck.builder()
                        .type(CheckType.SANCTIONS_OFAC)
                        .result(CheckResult.MATCH)
                        .matchScore(100)
                        .details(Map.of("listVersionId", SOME_LIST_VERSION_ID.toString()))
                        .build()))
                .listVersionId(SOME_LIST_VERSION_ID)
                .build();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("screeningId", "screenedAt", "durationMs")
                .isEqualTo(expected);
    }

    @Test
    void shouldHoldWhenWatchlistMatchScoresHundred() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(watchlistSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(WATCHLIST_HIT);
        given(onChainSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(NOVELTY_HIT);
        var service = new ScreeningService(sanctionsSetProvider,
                List.of(watchlistSignalProvider, onChainSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningResult.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict(Verdict.HOLD)
                .riskScore(100)
                .checks(List.of(WATCHLIST_HIT, NOVELTY_HIT))
                .listVersionId(SOME_LIST_VERSION_ID)
                .build();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("screeningId", "screenedAt", "durationMs")
                .isEqualTo(expected);
    }

    @Test
    void shouldHoldOnOneHopSanctionedInteractionWithoutBlocking() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(watchlistSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(CLEAR_WATCHLIST);
        given(onChainSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(INTERACTION_HIT);
        var service = new ScreeningService(sanctionsSetProvider,
                List.of(watchlistSignalProvider, onChainSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningResult.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict(Verdict.HOLD)
                .riskScore(70)
                .checks(List.of(CLEAR_WATCHLIST, INTERACTION_HIT))
                .listVersionId(SOME_LIST_VERSION_ID)
                .build();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("screeningId", "screenedAt", "durationMs")
                .isEqualTo(expected);
    }

    @Test
    void shouldPassWhenCleanAndBelowThreshold() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(watchlistSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(CLEAR_WATCHLIST);
        var service = new ScreeningService(sanctionsSetProvider, List.of(watchlistSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningResult.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict(Verdict.PASS)
                .riskScore(0)
                .checks(List.of(CLEAR_WATCHLIST))
                .listVersionId(SOME_LIST_VERSION_ID)
                .build();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("screeningId", "screenedAt", "durationMs")
                .isEqualTo(expected);
    }

    @Test
    void shouldPassWhenScoreIsOneBelowThreshold() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(onChainSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(SCORE_49);
        var service = new ScreeningService(sanctionsSetProvider, List.of(onChainSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.PASS);
        assertThat(result.riskScore()).isEqualTo(49);
    }

    @Test
    void shouldHoldWhenScoreEqualsThreshold() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(onChainSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(SCORE_50);
        var service = new ScreeningService(sanctionsSetProvider, List.of(onChainSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.HOLD);
        assertThat(result.riskScore()).isEqualTo(50);
    }

    @Test
    void shouldCapCombinedRiskScoreAtHundred() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(watchlistSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(WATCHLIST_HIT);
        given(onChainSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(NOVELTY_HIT);
        var service = new ScreeningService(sanctionsSetProvider,
                List.of(watchlistSignalProvider, onChainSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(result.riskScore()).isEqualTo(100);
    }

    @Test
    void shouldNormalizeChecksummedRecipientBeforeMatching() {
        // given
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        given(watchlistSignalProvider.provideSignal(SOME_RECIPIENT_ADDRESS)).willReturn(CLEAR_WATCHLIST);
        var service = new ScreeningService(sanctionsSetProvider, List.of(watchlistSignalProvider), HOLD_THRESHOLD);

        // when
        var result = service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS_CHECKSUMMED);

        // then
        assertThat(result.recipientAddress()).isEqualTo(SOME_RECIPIENT_ADDRESS);
    }

    @Test
    void shouldRejectMalformedAddress() {
        // given
        var service = new ScreeningService(sanctionsSetProvider, List.of(watchlistSignalProvider), HOLD_THRESHOLD);

        // when / then
        assertThatThrownBy(() -> service.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, "not-an-address"))
                .isInstanceOf(MalformedAddressException.class);
    }
}
