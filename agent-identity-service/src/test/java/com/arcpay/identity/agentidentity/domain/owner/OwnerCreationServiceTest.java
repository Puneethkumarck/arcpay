package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerCreationServiceTest {

    private final OwnerCreationService ownerCreationService = new OwnerCreationService();

    @Test
    void shouldCreateOwnerWithExpectedDeterministicFields() {
        // given
        var email = "alice@example.com";
        var walletAddress = "0x1234567890AbCdEf1234567890aBcDeF12345678";

        // when
        var result = ownerCreationService.createOwner(email, walletAddress);

        // then
        var owner = result.owner();
        var expected = Owner.builder()
                .ownerId(owner.ownerId())
                .email(email)
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash(owner.apiKeyHash())
                .status(OwnerStatus.ACTIVE)
                .createdAt(owner.createdAt())
                .updatedAt(owner.updatedAt())
                .build();
        assertThat(owner).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldCreateOwnerWithUuidV7() {
        // given / when
        var result = ownerCreationService.createOwner("alice@example.com", "0x1234567890abcdef1234567890abcdef12345678");

        // then
        assertThat(result.owner().ownerId().version()).isEqualTo(7);
    }

    @Test
    void shouldGenerateApiKeyWithCorrectPrefix() {
        // given / when
        var apiKey = ownerCreationService.generateApiKey();

        // then
        assertThat(apiKey).startsWith("ak_test_").hasSize(40);
    }

    @Test
    void shouldHashApiKeyWithSha256() throws Exception {
        // given
        var rawApiKey = "ak_test_abcdefghij1234567890abcdefghij12";

        // when
        var hash = ownerCreationService.hashApiKey(rawApiKey);

        // then
        var expectedDigest = MessageDigest.getInstance("SHA-256");
        var expectedHash = HexFormat.of().formatHex(
                expectedDigest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8)));
        assertThat(hash).isEqualTo(expectedHash);
    }

    @Test
    void shouldNormalizeWalletAddressToLowercase() {
        // given
        var mixedCaseWallet = "0x1234567890AbCdEf1234567890aBcDeF12345678";

        // when
        var result = ownerCreationService.createOwner("alice@example.com", mixedCaseWallet);

        // then
        assertThat(result.owner().walletAddress()).isEqualTo("0x1234567890abcdef1234567890abcdef12345678");
    }

    @Test
    void shouldReturnRawApiKeyDistinctFromHash() {
        // given / when
        var result = ownerCreationService.createOwner("alice@example.com", "0x1234567890abcdef1234567890abcdef12345678");

        // then
        assertThat(result.rawApiKey()).startsWith("ak_test_");
        assertThat(result.owner().apiKeyHash()).isNotEqualTo(result.rawApiKey());
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtToSameValue() {
        // given / when
        var result = ownerCreationService.createOwner("alice@example.com", "0x1234567890abcdef1234567890abcdef12345678");

        // then
        assertThat(result.owner().createdAt()).isEqualTo(result.owner().updatedAt());
    }
}
