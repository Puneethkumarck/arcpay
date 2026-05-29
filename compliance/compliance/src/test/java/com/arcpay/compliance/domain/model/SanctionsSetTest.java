package com.arcpay.compliance.domain.model;

import org.junit.jupiter.api.Test;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONS_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SanctionsSetTest {

    @Test
    void shouldExposeUnmodifiableAddresses() {
        // given
        var sanctionsSet = SOME_SANCTIONS_SET;

        // when / then
        assertThatThrownBy(() -> sanctionsSet.addresses().add("0xdeadbeef"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldContainSanctionedAddress() {
        // given
        var sanctionsSet = SOME_SANCTIONS_SET;

        // when / then
        assertThat(sanctionsSet.contains(SOME_SANCTIONED_ADDRESS)).isTrue();
    }
}
