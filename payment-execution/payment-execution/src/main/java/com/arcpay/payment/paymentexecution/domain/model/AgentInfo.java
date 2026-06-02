package com.arcpay.payment.paymentexecution.domain.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentInfo(UUID agentId, UUID ownerId, String status, String walletId, String walletAddress) {}
