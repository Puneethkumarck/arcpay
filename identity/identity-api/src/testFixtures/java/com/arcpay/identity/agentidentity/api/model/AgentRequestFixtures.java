package com.arcpay.identity.agentidentity.api.model;

public final class AgentRequestFixtures {

    public static final String SOME_AGENT_NAME = "shopping-agent-01";
    public static final String SOME_PURPOSE = "Automated USDC payments for e-commerce purchases";
    public static final String SOME_POLICY_HASH = "0x" + "a".repeat(64);

    public static final RegisterAgentRequest SOME_REGISTER_AGENT_REQUEST = new RegisterAgentRequest(
            SOME_AGENT_NAME,
            SOME_PURPOSE,
            SOME_POLICY_HASH
    );

    public static final RegisterAgentRequest SOME_REGISTER_AGENT_REQUEST_NO_POLICY = new RegisterAgentRequest(
            SOME_AGENT_NAME,
            SOME_PURPOSE,
            null
    );

    public static final UpdateAgentRequest SOME_UPDATE_AGENT_REQUEST = new UpdateAgentRequest(
            "updated-agent-name",
            "Updated purpose"
    );

    public static final UpdateAgentPolicyRequest SOME_UPDATE_POLICY_REQUEST = new UpdateAgentPolicyRequest(
            "0x" + "b".repeat(64)
    );

    private AgentRequestFixtures() {}
}
