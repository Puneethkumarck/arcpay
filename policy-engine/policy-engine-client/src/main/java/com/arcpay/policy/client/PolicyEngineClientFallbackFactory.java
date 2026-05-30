package com.arcpay.policy.client;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.UUID;

public class PolicyEngineClientFallbackFactory implements FallbackFactory<PolicyEngineClient> {

    @Override
    public PolicyEngineClient create(Throwable cause) {
        return new FailingPolicyEngineClient(cause);
    }

    private record FailingPolicyEngineClient(Throwable cause) implements PolicyEngineClient {

        @Override
        public PolicyEvaluationResponse reserve(ReserveRequest request) {
            throw mapFailure();
        }

        @Override
        public ReservationResponse commit(UUID paymentId) {
            throw mapFailure();
        }

        @Override
        public ReservationResponse release(UUID paymentId) {
            throw mapFailure();
        }

        @Override
        public ReservationResponse opsRelease(UUID paymentId) {
            throw mapFailure();
        }

        private RuntimeException mapFailure() {
            if (cause instanceof FeignException feignException && isClientError(feignException)) {
                return feignException;
            }
            return new PolicyEngineCallException("Policy service call failed", cause);
        }

        private boolean isClientError(FeignException feignException) {
            return feignException.status() >= 400 && feignException.status() < 500;
        }
    }
}
