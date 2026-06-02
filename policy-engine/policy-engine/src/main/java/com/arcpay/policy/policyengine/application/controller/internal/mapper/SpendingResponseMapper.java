package com.arcpay.policy.policyengine.application.controller.internal.mapper;

import com.arcpay.policy.policyengine.api.model.SpendingSummaryResponse;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpendingResponseMapper {

    default SpendingSummaryResponse toApi(UUID agentId, SpendingSummary summary) {
        return SpendingSummaryResponse.builder()
                .agentId(agentId)
                .dailyTotal(summary.dailyTotal())
                .weeklyTotal(summary.weeklyTotal())
                .monthlyTotal(summary.monthlyTotal())
                .transactionCount24h(summary.velocityCount())
                .lastTransactionAt(summary.lastTransactionAt())
                .build();
    }
}
