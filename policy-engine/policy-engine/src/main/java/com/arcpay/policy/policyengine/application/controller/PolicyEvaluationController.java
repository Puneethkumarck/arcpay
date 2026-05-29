package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.policy.policyengine.api.model.DryRunEvaluateRequest;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Validated
public class PolicyEvaluationController {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AgentServiceClient agentServiceClient;
    private final PolicyEvaluationService policyEvaluationService;
    private final EvaluationResponseMapper evaluationResponseMapper;

    @PostMapping("/evaluate")
    public PolicyEvaluationResponse evaluate(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody DryRunEvaluateRequest request) {
        log.info("Dry-run evaluation requested agentId={} ownerId={}", request.agentId(), principal.ownerId());
        verifyOwnershipAndActive(request.agentId(), principal.ownerId());
        var result = policyEvaluationService.evaluate(
                request.agentId(), request.recipientAddress(), request.amount(), Instant.now(), true);
        return evaluationResponseMapper.toApi(result);
    }

    private void verifyOwnershipAndActive(UUID agentId, UUID ownerId) {
        var agent = agentServiceClient.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new AgentOwnershipException(agentId, ownerId);
        }
        if (!ACTIVE_STATUS.equals(agent.status())) {
            throw new AgentNotActiveException(agentId, agent.status());
        }
    }
}
