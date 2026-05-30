package com.arcpay.payment.paymentexecution.domain.agent;

import com.arcpay.payment.paymentexecution.domain.exception.AgentNotActiveException;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentAccessDeniedException;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import com.arcpay.payment.paymentexecution.domain.port.AgentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentAuthorization {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AgentServiceClient agentServiceClient;

    public AgentInfo verifyOwnershipAndActive(UUID agentId, UUID ownerId) {
        var agent = agentServiceClient.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new PaymentAccessDeniedException(agentId, ownerId);
        }
        if (!ACTIVE_STATUS.equals(agent.status())) {
            throw new AgentNotActiveException(agentId, agent.status());
        }
        return agent;
    }
}
