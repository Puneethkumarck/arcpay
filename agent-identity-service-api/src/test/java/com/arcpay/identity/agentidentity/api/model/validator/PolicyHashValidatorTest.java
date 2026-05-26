package com.arcpay.identity.agentidentity.api.model.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyHashValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    record TestRecord(@ValidPolicyHash String hash) {}

    @Test
    void shouldAcceptValid0x64HexHash() {
        // given
        var hash = "0x" + "a".repeat(64);
        var record = new TestRecord(hash);

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectWrongLength() {
        // given
        var record = new TestRecord("0x" + "a".repeat(63));

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectTooLong() {
        // given
        var record = new TestRecord("0x" + "a".repeat(65));

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectMissing0xPrefix() {
        // given
        var record = new TestRecord("a".repeat(64));

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldAcceptNullForOptionalField() {
        // given
        var record = new TestRecord(null);

        // when
        var violations = validator.validate(record);

        // then
        assertThat(violations).isEmpty();
    }
}
