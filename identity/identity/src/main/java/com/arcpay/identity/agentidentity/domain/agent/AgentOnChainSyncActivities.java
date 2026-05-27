package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AgentOnChainSyncActivities {

    @ActivityMethod
    void syncToChain(AgentOnChainSyncRequest request);
}
