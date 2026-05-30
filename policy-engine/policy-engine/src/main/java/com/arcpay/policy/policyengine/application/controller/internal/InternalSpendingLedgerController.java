package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.SpendingSummaryResponse;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.SpendingResponseMapper;
import com.arcpay.policy.policyengine.domain.spending.SpendingLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Validated
public class InternalSpendingLedgerController {

    private static final int TWENTY_FOUR_HOURS_IN_MINUTES = 24 * 60;

    private final SpendingLedgerService spendingLedgerService;
    private final SpendingResponseMapper spendingResponseMapper;

    @GetMapping("/agents/{agentId}/spending-summary")
    public SpendingSummaryResponse getSpendingSummary(@PathVariable UUID agentId) {
        log.info("Internal spending summary agentId={}", agentId);
        var summary = spendingLedgerService.getSpendingSummary(agentId, TWENTY_FOUR_HOURS_IN_MINUTES);
        return spendingResponseMapper.toApi(agentId, summary);
    }
}
