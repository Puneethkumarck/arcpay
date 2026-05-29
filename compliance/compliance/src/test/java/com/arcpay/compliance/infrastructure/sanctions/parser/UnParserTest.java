package com.arcpay.compliance.infrastructure.sanctions.parser;

import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.UN_FEED;
import static org.assertj.core.api.Assertions.assertThat;

class UnParserTest {

    private final UnParser parser = new UnParser();

    @Test
    void shouldYieldNoAddressesForNameHeavyUnFeed() {
        // given
        var feed = UN_FEED;

        // when
        var records = parser.parse(feed);

        // then
        assertThat(records).isEmpty();
    }
}
