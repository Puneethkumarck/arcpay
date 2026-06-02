package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import java.util.UUID;

record OnChainAgentView(UUID owner, String metadataHash, String policyHash, boolean active, long createdAt) {}
