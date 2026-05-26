package com.arcpay.identity.agentidentity.fixtures;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;

import java.time.Instant;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;

public final class AgentFixtures {

    public static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    public static final String SOME_METADATA_HASH = "0x9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    public static final String SOME_POLICY_HASH = "0xabc1230000000000000000000000000000000000000000000000000000def456";
    public static final String SOME_WALLET_ID = "circle-wallet-abc123";
    public static final String SOME_WALLET_ADDRESS = "0xabcdef1234567890abcdef1234567890abcdef12";
    public static final String SOME_TX_HASH = "0xdeadbeef1234567890deadbeef1234567890deadbeef1234567890deadbeef12";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:05:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-06-01T10:05:00Z");

    public static final Agent SOME_AGENT_PROVISIONING = Agent.builder()
            .agentId(SOME_AGENT_ID)
            .ownerId(SOME_OWNER_ID)
            .name("shopping-agent-01")
            .purpose("Automated USDC payments for e-commerce purchases")
            .status(AgentStatus.PROVISIONING)
            .walletId(null)
            .walletAddress(null)
            .onChainTxHash(null)
            .policyHash(SOME_POLICY_HASH)
            .metadataHash(SOME_METADATA_HASH)
            .failureReason(null)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static final Agent SOME_AGENT_ACTIVE = Agent.builder()
            .agentId(UUID.fromString("019718a0-aaaa-7def-8000-aaaaaaaaaaaa"))
            .ownerId(SOME_OWNER_ID)
            .name("active-agent-01")
            .purpose("Active payment agent")
            .status(AgentStatus.ACTIVE)
            .walletId(SOME_WALLET_ID)
            .walletAddress(SOME_WALLET_ADDRESS)
            .onChainTxHash(SOME_TX_HASH)
            .policyHash(SOME_POLICY_HASH)
            .metadataHash(SOME_METADATA_HASH)
            .failureReason(null)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static final Agent SOME_AGENT_SUSPENDED = Agent.builder()
            .agentId(UUID.fromString("019718a0-bbbb-7def-8000-bbbbbbbbbbbb"))
            .ownerId(SOME_OWNER_ID)
            .name("suspended-agent-01")
            .purpose("Suspended payment agent")
            .status(AgentStatus.SUSPENDED)
            .walletId(SOME_WALLET_ID)
            .walletAddress(SOME_WALLET_ADDRESS)
            .onChainTxHash(SOME_TX_HASH)
            .policyHash(SOME_POLICY_HASH)
            .metadataHash(SOME_METADATA_HASH)
            .failureReason(null)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static final Agent SOME_AGENT_FAILED = Agent.builder()
            .agentId(UUID.fromString("019718a0-cccc-7def-8000-cccccccccccc"))
            .ownerId(SOME_OWNER_ID)
            .name("failed-agent-01")
            .purpose("Failed during provisioning")
            .status(AgentStatus.FAILED)
            .walletId(null)
            .walletAddress(null)
            .onChainTxHash(null)
            .policyHash(SOME_POLICY_HASH)
            .metadataHash(SOME_METADATA_HASH)
            .failureReason("Circle wallet creation failed after max retries")
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    private AgentFixtures() {}
}
