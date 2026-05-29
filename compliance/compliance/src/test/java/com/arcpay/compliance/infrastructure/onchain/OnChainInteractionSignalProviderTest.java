package com.arcpay.compliance.infrastructure.onchain;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAN_COUNTERPARTY;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONS_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OnChainInteractionSignalProviderTest {

    @Mock
    private UsdcTransferLogScanner scanner;

    @Mock
    private SanctionsSetProvider sanctionsSetProvider;

    @Test
    void shouldScoreSeventyWhenCounterpartyIsSanctioned() {
        // given
        given(scanner.counterpartiesOf(SOME_RECIPIENT_ADDRESS)).willReturn(Set.of(SOME_SANCTIONED_ADDRESS));
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        var provider = new OnChainInteractionSignalProvider(scanner, sanctionsSetProvider);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_INTERACTION)
                .result(CheckResult.FLAGGED)
                .matchScore(70)
                .details(Map.of("counterparty", SOME_SANCTIONED_ADDRESS, "txCount", 1))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldScoreZeroWhenNoCounterpartyIsSanctioned() {
        // given
        given(scanner.counterpartiesOf(SOME_RECIPIENT_ADDRESS)).willReturn(Set.of(SOME_CLEAN_COUNTERPARTY));
        given(sanctionsSetProvider.getCurrentSanctionsSet()).willReturn(SOME_SANCTIONS_SET);
        var provider = new OnChainInteractionSignalProvider(scanner, sanctionsSetProvider);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_INTERACTION)
                .result(CheckResult.CLEAR)
                .matchScore(0)
                .details(Map.of("counterparties", 1))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }
}
