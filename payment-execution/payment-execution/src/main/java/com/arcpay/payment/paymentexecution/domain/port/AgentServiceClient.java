package com.arcpay.payment.paymentexecution.domain.port;

import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;

import java.util.Optional;
import java.util.UUID;

public interface AgentServiceClient {

    Optional<AgentInfo> getAgent(UUID agentId);
}
