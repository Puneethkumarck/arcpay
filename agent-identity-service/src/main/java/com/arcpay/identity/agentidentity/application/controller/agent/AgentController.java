package com.arcpay.identity.agentidentity.application.controller.agent;

import com.arcpay.identity.agentidentity.api.model.AgentListResponse;
import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.ProvisioningStatusResponse;
import com.arcpay.identity.agentidentity.api.model.RegisterAgentRequest;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentRequest;
import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.application.controller.agent.mapper.AgentResponseMapper;
import com.arcpay.identity.agentidentity.application.security.OwnerPrincipal;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final AgentCommandHandler agentCommandHandler;
    private final AgentQueryHandler agentQueryHandler;
    private final AgentResponseMapper agentResponseMapper;
    private final IdempotencyHandler idempotencyHandler;

    @PostMapping
    public ResponseEntity<AgentResponse> registerAgent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RegisterAgentRequest request) {
        log.info("Agent registration requested name={} ownerId={}", request.name(), principal.ownerId());
        return idempotencyHandler.handle(
                idempotencyKey,
                principal.ownerId(),
                "POST /api/v1/agents",
                () -> {
                    var agent = agentCommandHandler.registerAgent(
                            principal.ownerId(), request.name(), request.purpose(), request.policyHash());
                    return agentResponseMapper.toApi(agent);
                },
                AgentResponse.class);
    }

    @GetMapping("/{agentId}")
    public AgentResponse getAgent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId) {
        var agent = agentQueryHandler.getAgent(agentId, principal.ownerId());
        return agentResponseMapper.toApi(agent);
    }

    @GetMapping
    public AgentListResponse listAgents(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(required = false) AgentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = agentQueryHandler.listAgents(principal.ownerId(), status, pageable);
        return agentResponseMapper.toApi(page);
    }

    @PutMapping("/{agentId}")
    public AgentResponse updateMetadata(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId,
            @Valid @RequestBody UpdateAgentRequest request) {
        log.info("Agent metadata update requested agentId={}", agentId);
        var agent = agentCommandHandler.updateMetadata(
                agentId, principal.ownerId(), request.name(), request.purpose());
        return agentResponseMapper.toApi(agent);
    }

    @PostMapping("/{agentId}/deactivate")
    public AgentResponse deactivate(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId) {
        log.info("Agent deactivation requested agentId={}", agentId);
        var agent = agentCommandHandler.deactivate(agentId, principal.ownerId());
        return agentResponseMapper.toApi(agent);
    }

    @PostMapping("/{agentId}/reactivate")
    public AgentResponse reactivate(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId) {
        log.info("Agent reactivation requested agentId={}", agentId);
        var agent = agentCommandHandler.reactivate(agentId, principal.ownerId());
        return agentResponseMapper.toApi(agent);
    }

    @GetMapping("/{agentId}/status")
    public ProvisioningStatusResponse getProvisioningStatus(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID agentId) {
        var status = agentQueryHandler.getProvisioningStatus(agentId, principal.ownerId());
        return agentResponseMapper.toApi(status);
    }
}
