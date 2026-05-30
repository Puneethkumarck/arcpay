package com.arcpay.policy.client;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "policy-engine", url = "${arcpay.policy-engine.url}")
public interface PolicyEngineClient {

    @PostMapping("/api/v1/internal/policies/reservations")
    PolicyEvaluationResponse reserve(@RequestBody ReserveRequest request);

    @PostMapping("/api/v1/internal/policies/reservations/{paymentId}/commit")
    ReservationResponse commit(@PathVariable UUID paymentId);

    @PostMapping("/api/v1/internal/policies/reservations/{paymentId}/release")
    ReservationResponse release(@PathVariable UUID paymentId);

    @PostMapping("/api/v1/internal/policies/reservations/{paymentId}/ops-release")
    ReservationResponse opsRelease(@PathVariable UUID paymentId);
}
