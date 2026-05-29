package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.policy.policyengine.api.model.CreatePolicyRequest;
import com.arcpay.policy.policyengine.api.model.PolicyListResponse;
import com.arcpay.policy.policyengine.api.model.PolicyResponse;
import com.arcpay.policy.policyengine.application.controller.mapper.PolicyResponseMapper;
import com.arcpay.policy.policyengine.domain.policy.PolicyCommandHandler;
import com.arcpay.policy.policyengine.domain.policy.PolicyQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/agents/{agentId}/policies")
@RequiredArgsConstructor
@Validated
public class PolicyController {

    private final PolicyCommandHandler policyCommandHandler;
    private final PolicyQueryHandler policyQueryHandler;
    private final PolicyResponseMapper policyResponseMapper;

    @PostMapping
    public ResponseEntity<PolicyResponse> createOrUpdatePolicy(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId,
            @Valid @RequestBody CreatePolicyRequest request) {
        log.info("Policy create/update requested agentId={} ownerId={}", agentId, principal.ownerId());
        var policy = policyCommandHandler.createOrUpdatePolicy(agentId, principal, request.rules());
        return ResponseEntity.status(HttpStatus.CREATED).body(policyResponseMapper.toApi(policy));
    }

    @GetMapping("/active")
    public PolicyResponse getActivePolicy(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId) {
        var policy = policyQueryHandler.getActivePolicy(agentId, principal.ownerId());
        return policyResponseMapper.toApi(policy);
    }

    @GetMapping("/{policyId}")
    public PolicyResponse getPolicy(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId,
            @PathVariable UUID policyId) {
        var policy = policyQueryHandler.getPolicy(agentId, policyId, principal.ownerId());
        return policyResponseMapper.toApi(policy);
    }

    @GetMapping
    public PolicyListResponse listPolicyHistory(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = policyQueryHandler.listPolicyHistory(agentId, principal.ownerId(), pageable);
        return policyResponseMapper.toApi(page);
    }
}
