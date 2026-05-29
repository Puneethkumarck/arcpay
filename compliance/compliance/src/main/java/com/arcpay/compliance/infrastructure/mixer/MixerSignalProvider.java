package com.arcpay.compliance.infrastructure.mixer;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class MixerSignalProvider implements RiskSignalProvider {

    private final Set<String> mixerAddresses;
    private final int mixerScore;

    MixerSignalProvider(
            @Value("${compliance.onchain.mixer-addresses:}") List<String> mixerAddresses,
            @Value("${compliance.onchain.mixer-score:50}") int mixerScore) {
        this.mixerAddresses = mixerAddresses == null
                ? Set.of()
                : mixerAddresses.stream()
                        .map(address -> address.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet());
        this.mixerScore = mixerScore;
    }

    @Override
    public ScreeningCheck provideSignal(String recipientAddress) {
        var matched = mixerAddresses.contains(recipientAddress);
        return ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_MIXER)
                .result(matched ? CheckResult.FLAGGED : CheckResult.CLEAR)
                .matchScore(matched ? mixerScore : 0)
                .details(matched
                        ? Map.of("knownMixer", Boolean.TRUE, "riskLevel", "high")
                        : Map.of("knownMixer", Boolean.FALSE))
                .build();
    }
}
