package com.arcpay.compliance.infrastructure.onchain;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAN_COUNTERPARTY;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OnChainNoveltySignalProviderTest {

    @Mock
    private UsdcTransferLogScanner scanner;

    @Test
    void shouldScoreTenWhenRecipientHasNoTransferHistory() {
        // given
        given(scanner.counterpartiesOf(SOME_RECIPIENT_ADDRESS)).willReturn(Set.of());
        var provider = new OnChainNoveltySignalProvider(scanner);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_NOVELTY)
                .result(CheckResult.LOW_RISK)
                .matchScore(10)
                .details(Map.of("transferCount", 0))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldScoreZeroWhenRecipientHasTransferHistory() {
        // given
        given(scanner.counterpartiesOf(SOME_RECIPIENT_ADDRESS)).willReturn(Set.of(SOME_CLEAN_COUNTERPARTY));
        var provider = new OnChainNoveltySignalProvider(scanner);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_NOVELTY)
                .result(CheckResult.CLEAR)
                .matchScore(0)
                .details(Map.of("transferCount", 1))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }
}
