package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.policy.policyengine.api.model.DryRunEvaluateRequest;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Validated
public class PolicyEvaluationController {

    private final PolicyEvaluationService policyEvaluationService;
    private final EvaluationResponseMapper evaluationResponseMapper;

    @PostMapping("/evaluate")
    public PolicyEvaluationResponse evaluate(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody DryRunEvaluateRequest request) {
        log.info("Dry-run evaluation requested agentId={} ownerId={}", request.agentId(), principal.ownerId());
        var result = policyEvaluationService.evaluateDryRunForOwner(
                principal.ownerId(), request.agentId(), request.recipientAddress(), request.amount());
        return evaluationResponseMapper.toApi(result);
    }
}
