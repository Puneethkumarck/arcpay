package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.InternalEvaluateRequest;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/policies")
@RequiredArgsConstructor
@Validated
public class InternalPolicyEvaluationController {

    private final AgentServiceClient agentServiceClient;
    private final PolicyEvaluationService policyEvaluationService;
    private final EvaluationResponseMapper evaluationResponseMapper;

    @PostMapping("/evaluate")
    public PolicyEvaluationResponse evaluate(@Valid @RequestBody InternalEvaluateRequest request) {
        log.info("Internal policy evaluation agentId={} amount={}", request.agentId(), request.amount());
        var agent = agentServiceClient.getAgent(request.agentId())
                .orElseThrow(() -> new AgentNotFoundException(request.agentId()));
        var result = policyEvaluationService.evaluate(
                request.agentId(),
                agent,
                request.recipientAddress(),
                request.amount(),
                request.requestedAt(),
                false);
        return evaluationResponseMapper.toApi(result);
    }
}
