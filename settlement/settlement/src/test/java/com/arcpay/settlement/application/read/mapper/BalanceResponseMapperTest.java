package com.arcpay.settlement.application.read.mapper;

import com.arcpay.settlement.api.model.BalanceResponse;
import org.junit.jupiter.api.Test;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_BALANCE_AMOUNT;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_USDC_TOKEN_ADDRESS;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someWalletBalance;
import static org.assertj.core.api.Assertions.assertThat;

class BalanceResponseMapperTest {

    private static final String SOME_AGENT_ID = "agent-789";

    private final BalanceResponseMapper mapper = new BalanceResponseMapper();

    @Test
    void shouldMapWalletBalanceToUsdcBalanceResponse() {
        // given
        var balance = someWalletBalance();

        // when
        var response = mapper.toApi(SOME_AGENT_ID, balance);

        // then
        var expected = BalanceResponse.builder()
                .agentId(SOME_AGENT_ID)
                .walletId(SOME_WALLET_ID)
                .tokenAddress(SOME_USDC_TOKEN_ADDRESS)
                .amount(SOME_BALANCE_AMOUNT)
                .currency("USDC")
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
