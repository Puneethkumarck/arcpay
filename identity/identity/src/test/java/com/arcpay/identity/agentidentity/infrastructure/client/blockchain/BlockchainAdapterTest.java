package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.math.BigInteger;
import java.util.UUID;

import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class BlockchainAdapterTest {

    private static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    private static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    private static final String CONTRACT_ADDRESS = "0xContractAddress";

    @Mock
    private Web3j web3j;

    @Mock
    private GasUsageRepository gasUsageRepository;

    @Mock
    @SuppressWarnings("rawtypes")
    private Request ethBlockNumberRequest;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRegisterAgentAndRecordGasUsage() throws Exception {
        // given
        var blockNumberResponse = new EthBlockNumber();
        blockNumberResponse.setResult("0x2a");
        given(web3j.ethBlockNumber()).willReturn(ethBlockNumberRequest);
        given(ethBlockNumberRequest.send()).willReturn(blockNumberResponse);
        var adapter = new BlockchainAdapter(web3j, gasUsageRepository, CONTRACT_ADDRESS);

        // when
        var result = adapter.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, "0xmetahash");

        // then
        assertThat(result.blockNumber()).isEqualTo(42L);
        assertThat(result.txHash()).startsWith("0x");
        then(gasUsageRepository).should().save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPassOwnerIdToGasUsageRecord() throws Exception {
        // given
        var blockNumberResponse = new EthBlockNumber();
        blockNumberResponse.setResult("0x1");
        given(web3j.ethBlockNumber()).willReturn(ethBlockNumberRequest);
        given(ethBlockNumberRequest.send()).willReturn(blockNumberResponse);
        var adapter = new BlockchainAdapter(web3j, gasUsageRepository, CONTRACT_ADDRESS);

        // when
        adapter.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, "0xmetahash");

        // then
        then(gasUsageRepository).should().save(
                org.mockito.ArgumentMatchers.argThat(gas ->
                        gas.ownerId().equals(SOME_OWNER_ID)
                                && gas.agentId().equals(SOME_AGENT_ID)
                                && "REGISTER_AGENT".equals(gas.operation())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowBlockchainRegistrationExceptionOnFailure() throws Exception {
        // given
        given(web3j.ethBlockNumber()).willReturn(ethBlockNumberRequest);
        given(ethBlockNumberRequest.send()).willThrow(new java.io.IOException("RPC timeout"));
        var adapter = new BlockchainAdapter(web3j, gasUsageRepository, CONTRACT_ADDRESS);

        // when / then
        assertThatThrownBy(() -> adapter.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, "0xmetahash"))
                .isInstanceOf(BlockchainRegistrationException.class)
                .hasMessageContaining(SOME_AGENT_ID.toString())
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void shouldReturnPlaceholderForDeactivateAgent() {
        // given
        var adapter = new BlockchainAdapter(web3j, gasUsageRepository, CONTRACT_ADDRESS);

        // when
        var result = adapter.deactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(result).contains(SOME_AGENT_ID.toString());
    }

    @Test
    void shouldReturnPlaceholderForReactivateAgent() {
        // given
        var adapter = new BlockchainAdapter(web3j, gasUsageRepository, CONTRACT_ADDRESS);

        // when
        var result = adapter.reactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(result).contains(SOME_AGENT_ID.toString());
    }
}
