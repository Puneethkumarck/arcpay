package com.arcpay.identity.agentidentity.domain.agent;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.UUID;

@ActivityInterface
public interface AgentProvisioningActivities {

    @ActivityMethod
    void createCircleWallet(UUID agentId);

    @ActivityMethod
    void registerOnChain(UUID agentId);

    @ActivityMethod
    void failProvisioning(UUID agentId, String failedStep, String reason);
}
