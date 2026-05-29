package com.arcpay.compliance.infrastructure.onchain;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;

import java.util.Map;

@Component
@ConditionalOnBean(Web3j.class)
@RequiredArgsConstructor
class OnChainInteractionSignalProvider implements RiskSignalProvider {

    private static final int INTERACTION_SCORE = 70;

    private final UsdcTransferLogScanner scanner;
    private final SanctionsSetProvider sanctionsSetProvider;

    @Override
    public ScreeningCheck provideSignal(String recipientAddress) {
        var counterparties = scanner.counterpartiesOf(recipientAddress);
        var sanctionsSet = sanctionsSetProvider.getCurrentSanctionsSet();
        var sanctionedCounterparty = sanctionsSet == null
                ? null
                : counterparties.stream().filter(sanctionsSet::contains).findFirst().orElse(null);

        if (sanctionedCounterparty == null) {
            return ScreeningCheck.builder()
                    .type(CheckType.ONCHAIN_INTERACTION)
                    .result(CheckResult.CLEAR)
                    .matchScore(0)
                    .details(Map.of("counterparties", counterparties.size()))
                    .build();
        }

        return ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_INTERACTION)
                .result(CheckResult.FLAGGED)
                .matchScore(INTERACTION_SCORE)
                .details(Map.of(
                        "counterparty", sanctionedCounterparty,
                        "txCount", counterparties.size()))
                .build();
    }
}
