package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.EXPECTED_NORMALIZED_EVM_ADDRESS;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.FEED_WITH_EVM_ADDRESS;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.SOME_OTHER_CHAIN_BTC_ADDRESS;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.UK_HMT_FEED;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UK_HMT;
import static org.assertj.core.api.Assertions.assertThat;

class UkHmtParserTest {

    private final UkHmtParser parser = new UkHmtParser();

    @Test
    void shouldYieldNoAddressesForNameHeavyUkHmtFeed() {
        // given
        var feed = UK_HMT_FEED;

        // when
        var records = parser.parse(feed);

        // then
        assertThat(records).isEmpty();
    }

    @Test
    void shouldExtractNormalizedDeduplicatedEvmAddress() {
        // given
        var feed = FEED_WITH_EVM_ADDRESS;

        // when
        var addresses = parser.parse(feed).stream().map(SanctionedAddressRecord::address).toList();

        // then
        assertThat(addresses).containsExactly(EXPECTED_NORMALIZED_EVM_ADDRESS);
    }

    @Test
    void shouldTagEveryRecordWithUkHmtSource() {
        // given
        var feed = FEED_WITH_EVM_ADDRESS;

        // when
        var sources = parser.parse(feed).stream().map(SanctionedAddressRecord::source).distinct().toList();

        // then
        assertThat(sources).containsExactly(UK_HMT);
    }

    @Test
    void shouldExcludeNamesDatesAndOtherChainAddresses() {
        // given
        var feed = FEED_WITH_EVM_ADDRESS;

        // when
        var addresses = parser.parse(feed).stream().map(SanctionedAddressRecord::address).toList();

        // then
        assertThat(addresses)
                .doesNotContain(SOME_OTHER_CHAIN_BTC_ADDRESS)
                .allMatch(address -> address.matches("0x[0-9a-f]{40}"));
    }
}
