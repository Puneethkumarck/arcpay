package com.arcpay.compliance.domain.port;

import com.arcpay.compliance.domain.model.ScreeningResult;

import java.util.UUID;

public interface ScreeningEngine {

    ScreeningResult screen(UUID paymentId, UUID agentId, String recipientAddress);
}
