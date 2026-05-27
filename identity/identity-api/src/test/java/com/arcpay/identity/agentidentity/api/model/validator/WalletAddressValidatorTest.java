package com.arcpay.identity.agentidentity.api.model.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAddressValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    record TestRecord(@ValidWalletAddress String address) {}

    @Test
    void shouldAcceptValid0xHexAddress() {
        // given
        var record = new TestRecord("0x1234567890abcdef1234567890abcdef12345678");

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAcceptEip55ChecksumAddress() {
        // given
        var record = new TestRecord("0x1234567890AbCdEf1234567890aBcDeF12345678");

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectMissing0xPrefix() {
        // given
        var record = new TestRecord("1234567890abcdef1234567890abcdef12345678");

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectWrongLength() {
        // given
        var record = new TestRecord("0x1234");

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectNonHexChars() {
        // given
        var record = new TestRecord("0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG");

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldAcceptNullForOptionalFields() {
        // given
        var record = new TestRecord(null);

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).isEmpty();
    }
}
