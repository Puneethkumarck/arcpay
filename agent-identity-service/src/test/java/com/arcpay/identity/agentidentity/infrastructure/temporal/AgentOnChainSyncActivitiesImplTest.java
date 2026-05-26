package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import com.arcpay.identity.agentidentity.domain.model.OnChainOperation;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_METADATA_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_POLICY_HASH;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AgentOnChainSyncActivitiesImplTest {

    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private AgentOnChainSyncActivitiesImpl activities;

    @Test
    void shouldCallDeactivateAgentForDeactivateOperation() {
        // given
        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.DEACTIVATE)
                .parameters(Map.of())
                .build();

        // when
        activities.syncToChain(request);

        // then
        then(blockchainService).should().deactivateAgent(SOME_AGENT_ID);
    }

    @Test
    void shouldCallReactivateAgentForReactivateOperation() {
        // given
        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.REACTIVATE)
                .parameters(Map.of())
                .build();

        // when
        activities.syncToChain(request);

        // then
        then(blockchainService).should().reactivateAgent(SOME_AGENT_ID);
    }

    @Test
    void shouldCallUpdateMetadataForUpdateMetadataOperation() {
        // given
        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.UPDATE_METADATA)
                .parameters(Map.of("metadataHash", SOME_METADATA_HASH))
                .build();

        // when
        activities.syncToChain(request);

        // then
        then(blockchainService).should().updateMetadata(SOME_AGENT_ID, SOME_METADATA_HASH);
    }

    @Test
    void shouldCallUpdatePolicyForUpdatePolicyOperation() {
        // given
        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.UPDATE_POLICY)
                .parameters(Map.of("policyHash", SOME_POLICY_HASH))
                .build();

        // when
        activities.syncToChain(request);

        // then
        then(blockchainService).should().updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);
    }
}
