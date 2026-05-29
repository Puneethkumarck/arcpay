package com.arcpay.compliance.infrastructure.watchlist;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import com.arcpay.compliance.domain.port.WatchlistStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class WatchlistSignalProvider implements RiskSignalProvider {

    private static final int WATCHLIST_SCORE = 100;

    private final WatchlistStore watchlistStore;

    @Override
    public ScreeningCheck provideSignal(String recipientAddress) {
        var matched = watchlistStore.contains(recipientAddress);
        return ScreeningCheck.builder()
                .type(CheckType.WATCHLIST)
                .result(matched ? CheckResult.MATCH : CheckResult.CLEAR)
                .matchScore(matched ? WATCHLIST_SCORE : 0)
                .details(matched
                        ? Map.of("matched", Boolean.TRUE, "address", recipientAddress)
                        : Map.of("matched", Boolean.FALSE))
                .build();
    }
}
