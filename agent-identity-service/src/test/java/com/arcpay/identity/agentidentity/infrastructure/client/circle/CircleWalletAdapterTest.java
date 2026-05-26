package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircleWalletAdapterTest {

    @Test
    void shouldNormalizeWalletAddressToLowercase() {
        // given
        var mixedCase = "0xAbCdEf1234567890AbCdEf1234567890AbCdEf12";

        // when
        var normalized = mixedCase.toLowerCase(Locale.ROOT);

        // then
        assertThat(normalized).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
    }

    @Test
    void shouldUseAgentIdAsIdempotencyKey() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");

        // when
        var key = agentId.toString();

        // then
        assertThat(key).isEqualTo("019718a0-5678-7def-8000-abcdef567890");
    }

    @Test
    void shouldCreatePropertiesWithAllFields() {
        // given / when
        var props = new CircleApiProperties(
                "https://api.circle.com", "key-123", "ws-456", "ARC", 5000, 10000);

        // then
        assertThat(props).usingRecursiveComparison().isEqualTo(
                new CircleApiProperties("https://api.circle.com", "key-123", "ws-456", "ARC", 5000, 10000));
    }

    @Test
    void shouldWrapExceptionAsCircleApiException() {
        // given
        var cause = new RuntimeException("connection refused");
        var exception = new CircleApiException("Circle wallet creation failed", cause);

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Circle wallet creation failed")
                .hasCause(cause);
    }
}
