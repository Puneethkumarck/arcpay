package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.EXPECTED_OFAC_NONSDN_ADDRESSES;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.OFAC_NONSDN_FEED;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_NONSDN;
import static org.assertj.core.api.Assertions.assertThat;

class OfacNonSdnParserTest {

    private final OfacNonSdnParser parser = new OfacNonSdnParser();

    @Test
    void shouldExtractNormalizedEvmAddressesAndSkipOtherChains() {
        // given
        var feed = OFAC_NONSDN_FEED;

        // when
        var addresses = parser.parse(feed).stream().map(SanctionedAddressRecord::address).toList();

        // then
        assertThat(addresses).containsExactlyInAnyOrderElementsOf(EXPECTED_OFAC_NONSDN_ADDRESSES);
    }

    @Test
    void shouldTagEveryRecordWithItsSource() {
        // given
        var feed = OFAC_NONSDN_FEED;

        // when
        var sources = parser.parse(feed).stream().map(SanctionedAddressRecord::source).distinct().toList();

        // then
        assertThat(sources).containsExactly(OFAC_NONSDN);
    }
}
