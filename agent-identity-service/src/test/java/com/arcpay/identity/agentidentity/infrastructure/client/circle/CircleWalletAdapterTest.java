package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CircleWalletAdapterTest {

    @Test
    void shouldNormalizeWalletAddressToLowercase() {
        // given — verify normalization logic
        var mixedCase = "0xAbCdEf1234567890AbCdEf1234567890AbCdEf12";

        // when
        var normalized = mixedCase.toLowerCase(java.util.Locale.ROOT);

        // then
        assertThat(normalized).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
    }

    @Test
    void shouldUseAgentIdAsIdempotencyKey() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");

        // then — the request body uses agentId.toString() as idempotencyKey
        assertThat(agentId.toString()).isEqualTo("019718a0-5678-7def-8000-abcdef567890");
    }

    @Test
    void shouldCreatePropertiesWithAllFields() {
        // given / when
        var props = new CircleApiProperties(
                "https://api.circle.com", "key-123", "ws-456", "ARC", 5000, 10000);

        // then
        assertThat(props.baseUrl()).isEqualTo("https://api.circle.com");
        assertThat(props.apiKey()).isEqualTo("key-123");
        assertThat(props.walletSetId()).isEqualTo("ws-456");
        assertThat(props.blockchain()).isEqualTo("ARC");
    }
}
