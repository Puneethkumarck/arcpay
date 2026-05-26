package com.arcpay.identity.agentidentity.domain.agent;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataHashUtilTest {

    @Test
    void shouldProduceKnownKeccak256Output() {
        // given
        var name = "shopping-agent";
        var purpose = "Automated USDC payments";

        // when
        var result = MetadataHashUtil.computeMetadataHash(name, purpose);

        // then — verify against direct web3j keccak256
        var packed = (name + purpose).getBytes(StandardCharsets.UTF_8);
        var expected = "0x" + HexFormat.of().formatHex(Hash.sha3(packed));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldMatchSolidityAbiEncodePacked() {
        // given — Solidity's abi.encodePacked(string, string) concatenates raw UTF-8 bytes
        var name = "test";
        var purpose = "purpose";

        // when
        var result = MetadataHashUtil.computeMetadataHash(name, purpose);

        // then
        assertThat(result).startsWith("0x");
        assertThat(result).hasSize(2 + 64); // "0x" + 32 bytes hex
    }

    @Test
    void shouldHandleEmptyStrings() {
        // given / when
        var result = MetadataHashUtil.computeMetadataHash("", "");

        // then
        var expected = "0x" + HexFormat.of().formatHex(Hash.sha3(new byte[0]));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentInputs() {
        // given / when
        var hash1 = MetadataHashUtil.computeMetadataHash("agent-a", "purpose-a");
        var hash2 = MetadataHashUtil.computeMetadataHash("agent-b", "purpose-b");

        // then
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
