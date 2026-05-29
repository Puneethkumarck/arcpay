package com.arcpay.compliance.infrastructure.watchlist;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.port.WatchlistStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchlistSignalProviderTest {

    @Mock
    private WatchlistStore watchlistStore;

    @Test
    void shouldScoreHundredWhenRecipientIsWatchlisted() {
        // given
        given(watchlistStore.contains(SOME_WATCHLIST_ADDRESS)).willReturn(true);
        var provider = new WatchlistSignalProvider(watchlistStore);

        // when
        var check = provider.provideSignal(SOME_WATCHLIST_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.WATCHLIST)
                .result(CheckResult.MATCH)
                .matchScore(100)
                .details(Map.of("matched", Boolean.TRUE, "address", SOME_WATCHLIST_ADDRESS))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldScoreZeroWhenRecipientIsNotWatchlisted() {
        // given
        given(watchlistStore.contains(SOME_RECIPIENT_ADDRESS)).willReturn(false);
        var provider = new WatchlistSignalProvider(watchlistStore);

        // when
        var check = provider.provideSignal(SOME_RECIPIENT_ADDRESS);

        // then
        var expected = ScreeningCheck.builder()
                .type(CheckType.WATCHLIST)
                .result(CheckResult.CLEAR)
                .matchScore(0)
                .details(Map.of("matched", Boolean.FALSE))
                .build();
        assertThat(check).usingRecursiveComparison().isEqualTo(expected);
    }
}
