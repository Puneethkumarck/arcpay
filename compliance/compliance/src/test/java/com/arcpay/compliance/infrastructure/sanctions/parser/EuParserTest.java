package com.arcpay.compliance.infrastructure.sanctions.parser;

import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.EU_FEED;
import static org.assertj.core.api.Assertions.assertThat;

class EuParserTest {

    private final EuParser parser = new EuParser();

    @Test
    void shouldYieldNoAddressesForNameHeavyEuFeed() {
        // given
        var feed = EU_FEED;

        // when
        var records = parser.parse(feed);

        // then
        assertThat(records).isEmpty();
    }
}
