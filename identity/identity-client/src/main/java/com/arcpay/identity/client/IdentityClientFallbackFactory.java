package com.arcpay.identity.client;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Optional;
import java.util.UUID;

public class IdentityClientFallbackFactory implements FallbackFactory<IdentityServiceClient> {

    @Override
    public IdentityServiceClient create(Throwable cause) {
        return new FailingIdentityServiceClient(cause);
    }

    private record FailingIdentityServiceClient(Throwable cause) implements IdentityServiceClient {

        @Override
        public Optional<OwnerPrincipalResponse> resolveApiKey(String hash) {
            return Optional.empty();
        }

        @Override
        public AgentResponse getAgent(UUID agentId) {
            throw mapFailure();
        }

        @Override
        public AgentResponse updatePolicy(UUID agentId, UpdateAgentPolicyRequest request) {
            throw mapFailure();
        }

        private RuntimeException mapFailure() {
            if (cause instanceof FeignException.NotFound notFound) {
                return notFound;
            }
            return new IdentityServiceCallException("Identity service call failed", cause);
        }
    }
}
