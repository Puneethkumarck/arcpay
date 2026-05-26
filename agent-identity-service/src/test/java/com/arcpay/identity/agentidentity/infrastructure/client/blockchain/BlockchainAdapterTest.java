package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockchainAdapterTest {

    @Test
    void shouldConstructCorrectBytes32FromUuid() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");

        // when
        var bytes32 = UuidConversionUtil.uuidToBytes32(agentId);

        // then
        assertThat(bytes32).hasSize(32);
        for (int i = 0; i < 16; i++) {
            assertThat(bytes32[i]).isZero();
        }
    }

    @Test
    void shouldRoundTripUuidThroughBytes32() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");

        // when
        var bytes32 = UuidConversionUtil.uuidToBytes32(agentId);
        var recovered = UuidConversionUtil.bytes32ToUuid(bytes32);

        // then
        assertThat(recovered).isEqualTo(agentId);
    }

    @Test
    void shouldRejectNullUuidInToBytes32() {
        // given / when / then
        assertThatThrownBy(() -> UuidConversionUtil.uuidToBytes32(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNonCanonicalBytes32() {
        // given
        var bytes32 = new byte[32];
        bytes32[0] = 1;

        // when / then
        assertThatThrownBy(() -> UuidConversionUtil.bytes32ToUuid(bytes32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Non-canonical");
    }

    @Test
    void shouldCreateBlockchainPropertiesWithAllFields() {
        // given / when
        var props = new BlockchainProperties("http://localhost:8545", 5042002L, "0xprivatekey");

        // then
        assertThat(props).usingRecursiveComparison().isEqualTo(
                new BlockchainProperties("http://localhost:8545", 5042002L, "0xprivatekey"));
    }
}
