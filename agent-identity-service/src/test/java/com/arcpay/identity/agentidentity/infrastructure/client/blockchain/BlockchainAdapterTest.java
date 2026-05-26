package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldHaveCorrectPortMethodSignatures() {
        // given - verify the port interface compiles and has expected methods
        BlockchainService service = new BlockchainService() {
            @Override
            public RegistrationResult registerAgent(UUID agentId, UUID ownerId, String metadataHash) {
                return new RegistrationResult("0xtxhash", 42L);
            }

            @Override
            public String deactivateAgent(UUID agentId) {
                return "0xdeactivate";
            }

            @Override
            public String reactivateAgent(UUID agentId) {
                return "0xreactivate";
            }

            @Override
            public String updateMetadata(UUID agentId, String metadataHash) {
                return "0xupdate";
            }

            @Override
            public String updatePolicy(UUID agentId, String policyHash) {
                return "0xpolicy";
            }

            @Override
            public boolean isAgentActive(UUID agentId) {
                return true;
            }
        };

        // when
        var result = service.registerAgent(UUID.randomUUID(), UUID.randomUUID(), "0xhash");

        // then
        assertThat(result.txHash()).isEqualTo("0xtxhash");
        assertThat(result.blockNumber()).isEqualTo(42L);
    }
}
