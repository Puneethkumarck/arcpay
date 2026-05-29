package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class IdentityServiceAdapter implements AgentServiceClient {

    private final IdentityServiceClient identityClient;
    private final AgentInfoMapper agentInfoMapper;

    @Override
    public Optional<AgentInfo> getAgent(UUID agentId) {
        try {
            var response = identityClient.getAgent(agentId);
            return Optional.of(agentInfoMapper.toDomain(response));
        } catch (FeignException.NotFound e) {
            log.debug("Agent not found in Identity Service");
            throw new AgentNotFoundException(agentId);
        } catch (com.arcpay.identity.client.IdentityServiceUnavailableException e) {
            throw new IdentityServiceUnavailableException("Identity service call failed", e);
        }
    }

    @Override
    public void updatePolicy(UUID agentId, String policyHash) {
        try {
            identityClient.updatePolicy(agentId, new UpdateAgentPolicyRequest(policyHash));
        } catch (com.arcpay.identity.client.IdentityServiceUnavailableException e) {
            throw new IdentityServiceUnavailableException("Identity service call failed", e);
        }
    }
}
