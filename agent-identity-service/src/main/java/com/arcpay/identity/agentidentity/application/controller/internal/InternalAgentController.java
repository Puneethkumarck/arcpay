package com.arcpay.identity.agentidentity.application.controller.internal;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.agentidentity.application.controller.agent.mapper.AgentResponseMapper;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/agents")
@RequiredArgsConstructor
@Validated
public class InternalAgentController {

    private final AgentCommandHandler agentCommandHandler;
    private final AgentResponseMapper agentResponseMapper;
    private final AgentRepository agentRepository;

    @PutMapping("/{agentId}/policy")
    public AgentResponse updatePolicy(
            @PathVariable UUID agentId,
            @Valid @RequestBody UpdateAgentPolicyRequest request) {
        log.info("Internal policy update requested agentId={}", agentId);
        var agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException(agentId));
        var updated = agentCommandHandler.updatePolicy(agentId, agent.ownerId(), request.policyHash());
        return agentResponseMapper.toApi(updated);
    }
}
