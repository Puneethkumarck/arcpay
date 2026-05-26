package com.arcpay.identity.agentidentity.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTest {

    @ParameterizedTest
    @MethodSource("nullRequiredFields")
    void shouldRejectNullRequiredField(String fieldName, UnaryOperator<Agent.AgentBuilder> mutator) {
        // given
        var builder = mutator.apply(SOME_AGENT_ACTIVE.toBuilder());

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    static Stream<Arguments> nullRequiredFields() {
        return Stream.of(
                Arguments.of("agentId", (UnaryOperator<Agent.AgentBuilder>) b -> b.agentId(null)),
                Arguments.of("ownerId", (UnaryOperator<Agent.AgentBuilder>) b -> b.ownerId(null)),
                Arguments.of("name", (UnaryOperator<Agent.AgentBuilder>) b -> b.name(null)),
                Arguments.of("purpose", (UnaryOperator<Agent.AgentBuilder>) b -> b.purpose(null)),
                Arguments.of("status", (UnaryOperator<Agent.AgentBuilder>) b -> b.status(null)),
                Arguments.of("metadataHash", (UnaryOperator<Agent.AgentBuilder>) b -> b.metadataHash(null)),
                Arguments.of("createdAt", (UnaryOperator<Agent.AgentBuilder>) b -> b.createdAt(null)),
                Arguments.of("updatedAt", (UnaryOperator<Agent.AgentBuilder>) b -> b.updatedAt(null))
        );
    }

    @Test
    void shouldAcceptNullOptionalFieldsForProvisioningAgent() {
        // given / when
        var agent = SOME_AGENT_PROVISIONING;

        // then
        assertThat(agent.walletId()).isNull();
        assertThat(agent.walletAddress()).isNull();
        assertThat(agent.onChainTxHash()).isNull();
        assertThat(agent.failureReason()).isNull();
    }

    @Test
    void shouldTransitionToWalletReadyViaWithWallet() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when
        var result = agent.withWallet("wallet-xyz", "0xabcabcabcabcabcabcabcabcabcabcabcabcabca");

        // then
        var expected = agent.toBuilder()
                .walletId("wallet-xyz")
                .walletAddress("0xabcabcabcabcabcabcabcabcabcabcabcabcabca")
                .status(AgentStatus.WALLET_READY)
                .updatedAt(result.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionToActiveViaWithOnChainRegistration() {
        // given
        var agent = SOME_AGENT_ACTIVE.toBuilder().status(AgentStatus.WALLET_READY).onChainTxHash(null).build();

        // when
        var result = agent.withOnChainRegistration("0xdeadbeef");

        // then
        var expected = agent.toBuilder()
                .onChainTxHash("0xdeadbeef")
                .status(AgentStatus.ACTIVE)
                .updatedAt(result.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionToFailedViaWithFailure() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when
        var result = agent.withFailure("Circle wallet creation timed out");

        // then
        var expected = agent.toBuilder()
                .failureReason("Circle wallet creation timed out")
                .status(AgentStatus.FAILED)
                .updatedAt(result.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectNullWalletIdInWithWallet() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when / then
        assertThatThrownBy(() -> agent.withWallet(null, "0xabc"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void shouldRejectNullWalletAddressInWithWallet() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when / then
        assertThatThrownBy(() -> agent.withWallet("wallet-123", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletAddress");
    }

    @Test
    void shouldRejectNullTxHashInWithOnChainRegistration() {
        // given
        var agent = SOME_AGENT_ACTIVE;

        // when / then
        assertThatThrownBy(() -> agent.withOnChainRegistration(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("txHash");
    }

    @Test
    void shouldRejectNullReasonInWithFailure() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when / then
        assertThatThrownBy(() -> agent.withFailure(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reason");
    }
}
