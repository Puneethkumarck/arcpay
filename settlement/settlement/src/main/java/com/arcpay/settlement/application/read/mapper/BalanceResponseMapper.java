package com.arcpay.settlement.application.read.mapper;

import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.domain.model.WalletBalance;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper {

    private static final String USDC = "USDC";

    public BalanceResponse toApi(String agentId, WalletBalance balance) {
        return BalanceResponse.builder()
                .agentId(agentId)
                .walletId(balance.walletId())
                .tokenAddress(balance.tokenAddress())
                .amount(balance.amount())
                .currency(USDC)
                .build();
    }
}
