package com.arcpay.identity.agentidentity.api.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterOwnerRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRequest() {
        // given
        var request = new RegisterOwnerRequest(
                "alice@example.com",
                "0x1234567890abcdef1234567890abcdef12345678"
        );

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankEmail(String email) {
        // given
        var request = new RegisterOwnerRequest(email, "0x1234567890abcdef1234567890abcdef12345678");

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldRejectBlankWalletAddress(String wallet) {
        // given
        var request = new RegisterOwnerRequest("alice@example.com", wallet);

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidWalletAddress() {
        // given
        var request = new RegisterOwnerRequest("alice@example.com", "not-a-wallet");

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("wallet address");
    }
}
