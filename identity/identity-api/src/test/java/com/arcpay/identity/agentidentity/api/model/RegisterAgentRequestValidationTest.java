package com.arcpay.identity.agentidentity.api.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterAgentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRequest() {
        // given
        var request = new RegisterAgentRequest(
                "shopping-agent",
                "Automated USDC payments",
                "0x" + "a".repeat(64)
        );

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAcceptRequestWithoutPolicyHash() {
        // given
        var request = new RegisterAgentRequest("shopping-agent", "Automated USDC payments", null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankName(String name) {
        // given
        var request = new RegisterAgentRequest(name, "purpose", null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectNameTooShort() {
        // given
        var request = new RegisterAgentRequest("ab", "purpose", null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectNameTooLong() {
        // given
        var request = new RegisterAgentRequest("a".repeat(65), "purpose", null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankPurpose(String purpose) {
        // given
        var request = new RegisterAgentRequest("valid-name", purpose, null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectPurposeTooLong() {
        // given
        var request = new RegisterAgentRequest("valid-name", "a".repeat(257), null);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidPolicyHash() {
        // given
        var request = new RegisterAgentRequest("valid-name", "valid purpose", "0xbad");

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("policy hash");
    }
}
