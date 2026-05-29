package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.EXPECTED_OFAC_SDN_ADDRESSES;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.OFAC_SDN_FEED;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.SOME_OTHER_CHAIN_BTC_ADDRESS;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static org.assertj.core.api.Assertions.assertThat;

class OfacSdnParserTest {

    private final OfacSdnParser parser = new OfacSdnParser();

    @Test
    void shouldExtractOnlyNormalizedEvmAddresses() {
        // given
        var feed = OFAC_SDN_FEED;

        // when
        var addresses = parser.parse(feed).stream().map(SanctionedAddressRecord::address).toList();

        // then
        assertThat(addresses).containsExactlyInAnyOrderElementsOf(EXPECTED_OFAC_SDN_ADDRESSES);
    }

    @Test
    void shouldTagEveryRecordWithItsSource() {
        // given
        var feed = OFAC_SDN_FEED;

        // when
        var sources = parser.parse(feed).stream().map(SanctionedAddressRecord::source).distinct().toList();

        // then
        assertThat(sources).containsExactly(OFAC_SDN);
    }

    @Test
    void shouldExcludeNamesDatesAndOtherChainAddresses() {
        // given
        var feed = OFAC_SDN_FEED;

        // when
        var addresses = parser.parse(feed).stream().map(SanctionedAddressRecord::address).toList();

        // then
        assertThat(addresses)
                .doesNotContain(SOME_OTHER_CHAIN_BTC_ADDRESS)
                .noneMatch(address -> address.contains("LAZARUS"))
                .allMatch(address -> address.matches("0x[0-9a-f]{40}"));
    }
}
