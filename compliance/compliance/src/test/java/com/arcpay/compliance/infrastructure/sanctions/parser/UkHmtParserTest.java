package com.arcpay.compliance.infrastructure.sanctions.parser;

import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.UK_HMT_FEED;
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
}
