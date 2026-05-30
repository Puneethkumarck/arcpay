package com.arcpay.settlement.application.read;

import com.arcpay.settlement.application.controller.GlobalExceptionHandler;
import com.arcpay.settlement.application.read.mapper.BalanceResponseMapper;
import com.arcpay.settlement.application.read.mapper.TransferStatusResponseMapperImpl;
import com.arcpay.settlement.domain.SettlementQueryService;
import com.arcpay.settlement.domain.TransferNotFoundException;
import com.arcpay.settlement.domain.model.TransferState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someSettlementTransaction;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someWalletBalance;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class InternalReadControllerTest {

    @Mock
    private SettlementQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalReadController(
                        queryService,
                        new TransferStatusResponseMapperImpl(),
                        new BalanceResponseMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnUsdcBalanceForAgent() throws Exception {
        // given
        given(queryService.balanceFor(SOME_WALLET_ID)).willReturn(someWalletBalance());

        // when
        mockMvc.perform(get("/api/v1/internal/wallets/{agentId}/balance", SOME_WALLET_ID))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.walletId").value(SOME_WALLET_ID))
                .andExpect(jsonPath("$.currency").value("USDC"))
                .andExpect(jsonPath("$.tokenAddress").value("0x3600000000000000000000000000000000000000"))
                .andExpect(jsonPath("$.amount").value(120.500000));
    }

    @Test
    void shouldReturnTransferStatusWhenFound() throws Exception {
        // given
        given(queryService.findTransfer(SOME_PAYMENT_ID))
                .willReturn(someSettlementTransaction(TransferState.COMPLETED));

        // when
        mockMvc.perform(get("/api/v1/internal/transfers/{paymentId}", SOME_PAYMENT_ID))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(SOME_PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.circleTxId").value(SOME_CIRCLE_TX_ID))
                .andExpect(jsonPath("$.state").value("COMPLETED"));
    }

    @Test
    void shouldReturnNotFoundWhenTransferAbsent() throws Exception {
        // given
        var randomId = UUID.randomUUID();
        willThrow(new TransferNotFoundException("Settlement transaction not found for paymentId=" + randomId))
                .given(queryService).findTransfer(randomId);

        // when
        mockMvc.perform(get("/api/v1/internal/transfers/{paymentId}", randomId))
                // then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCPAY-SETTLEMENT-0001"));
    }
}
