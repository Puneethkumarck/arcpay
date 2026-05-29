package com.arcpay.compliance.infrastructure.onchain;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;

import java.util.Map;

@Component
@ConditionalOnBean(Web3j.class)
@RequiredArgsConstructor
class OnChainNoveltySignalProvider implements RiskSignalProvider {

    private static final int NOVELTY_SCORE = 10;

    private final UsdcTransferLogScanner scanner;

    @Override
    public ScreeningCheck provideSignal(String recipientAddress) {
        var counterparties = scanner.counterpartiesOf(recipientAddress);
        var novel = counterparties.isEmpty();
        return ScreeningCheck.builder()
                .type(CheckType.ONCHAIN_NOVELTY)
                .result(novel ? CheckResult.LOW_RISK : CheckResult.CLEAR)
                .matchScore(novel ? NOVELTY_SCORE : 0)
                .details(Map.of("transferCount", counterparties.size()))
                .build();
    }
}
