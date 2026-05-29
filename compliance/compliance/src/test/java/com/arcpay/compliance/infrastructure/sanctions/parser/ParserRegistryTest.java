package com.arcpay.compliance.infrastructure.sanctions.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.EU;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_NONSDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UK_HMT;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserRegistryTest {

    private final ParserRegistry registry = new ParserRegistry(List.of(
            new OfacSdnParser(),
            new OfacNonSdnParser(),
            new UnParser(),
            new EuParser(),
            new UkHmtParser()));

    @Test
    void shouldDispatchToTheParserForEachSource() {
        // given / when / then
        assertThat(registry.parserFor(OFAC_SDN).source()).isEqualTo(OFAC_SDN);
        assertThat(registry.parserFor(OFAC_NONSDN).source()).isEqualTo(OFAC_NONSDN);
        assertThat(registry.parserFor(UN).source()).isEqualTo(UN);
        assertThat(registry.parserFor(EU).source()).isEqualTo(EU);
        assertThat(registry.parserFor(UK_HMT).source()).isEqualTo(UK_HMT);
    }

    @Test
    void shouldRejectUnregisteredSource() {
        // given
        var sparseRegistry = new ParserRegistry(List.of(new OfacSdnParser()));

        // when / then
        assertThatThrownBy(() -> sparseRegistry.parserFor(UN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
