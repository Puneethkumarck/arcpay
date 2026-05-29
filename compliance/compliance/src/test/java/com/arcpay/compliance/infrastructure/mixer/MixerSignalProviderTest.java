package com.arcpay.compliance.infrastructure.mixer;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_MIXER_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

class MixerSignalProviderTest {

    @Test
    void shouldFlagWhenRecipientIsConfiguredMixer() {
        // given
        var provider = new MixerSignalProvider(List.of(SOME_MIXER_ADDRESS), 50);

        // when
        var check = provider.provideSignal(SOME_MIXER_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_MIXER)
                .result(CheckResult.FLAGGED)
                .matchScore(50)
                .details(Map.of("knownMixer", Boolean.TRUE, "riskLevel", "high"))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldClearWhenMixerListIsEmpty() {
        // given
        var provider = new MixerSignalProvider(List.of(), 50);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_MIXER)
                .result(CheckResult.CLEAR)
                .matchScore(0)
                .details(Map.of("knownMixer", Boolean.FALSE))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }
}
