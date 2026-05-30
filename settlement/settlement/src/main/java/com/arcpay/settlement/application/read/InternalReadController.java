package com.arcpay.settlement.application.read;

import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import com.arcpay.settlement.application.read.mapper.BalanceResponseMapper;
import com.arcpay.settlement.application.read.mapper.TransferStatusResponseMapper;
import com.arcpay.settlement.domain.SettlementQueryService;
import jakarta.validation.constraints.NotBlank;
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
public class InternalReadController {

    private final SettlementQueryService queryService;
    private final TransferStatusResponseMapper transferStatusResponseMapper;
    private final BalanceResponseMapper balanceResponseMapper;

    @GetMapping("/wallets/{agentId}/balance")
    public BalanceResponse balance(@PathVariable @NotBlank String agentId) {
        log.info("Balance query agentId={}", agentId);
        return balanceResponseMapper.toApi(agentId, queryService.balanceFor(agentId));
    }

    @GetMapping("/transfers/{paymentId}")
    public TransferStatusResponse transferStatus(@PathVariable UUID paymentId) {
        log.info("Transfer status query paymentId={}", paymentId);
        return transferStatusResponseMapper.toApi(queryService.findTransfer(paymentId));
    }
}
