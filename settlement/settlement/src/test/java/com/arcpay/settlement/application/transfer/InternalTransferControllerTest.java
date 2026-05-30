package com.arcpay.settlement.application.transfer;

import com.arcpay.settlement.application.controller.GlobalExceptionHandler;
import com.arcpay.settlement.application.transfer.mapper.TransferInitiatedResponseMapper;
import com.arcpay.settlement.application.transfer.mapper.TransferRequestMapperImpl;
import com.arcpay.settlement.domain.InsufficientBalanceException;
import com.arcpay.settlement.domain.TransferSubmissionService;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.model.TransferSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalTransferControllerTest {

    private static final String VALID_REQUEST = """
            {
              "paymentId": "%s",
              "walletId": "%s",
              "recipientAddress": "%s",
              "amount": "25.00",
              "memo": "invoice-42"
            }
            """.formatted(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT_ADDRESS);

    private static final String INVALID_REQUEST = """
            {
              "paymentId": "%s",
              "walletId": " ",
              "recipientAddress": "%s",
              "amount": "25.00"
            }
            """.formatted(SOME_PAYMENT_ID, SOME_RECIPIENT_ADDRESS);

    @Mock
    private TransferSubmissionService transferSubmissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalTransferController(
                        transferSubmissionService,
                        new TransferRequestMapperImpl(),
                        new TransferInitiatedResponseMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldSubmitTransferAndReturnInitiatedResponse() throws Exception {
        // given
        given(transferSubmissionService.submit(argThat(
                command -> command != null && SOME_PAYMENT_ID.equals(command.paymentId()))))
                .willReturn(new TransferSubmission(SOME_CIRCLE_TX_ID, TransferState.INITIATED));

        // when
        mockMvc.perform(post("/api/v1/internal/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(SOME_PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.circleTxId").value(SOME_CIRCLE_TX_ID))
                .andExpect(jsonPath("$.state").value("INITIATED"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenBalanceInsufficient() throws Exception {
        // given
        willThrow(new InsufficientBalanceException("Insufficient balance for paymentId=" + SOME_PAYMENT_ID))
                .given(transferSubmissionService).submit(argThat(
                        command -> command != null && SOME_PAYMENT_ID.equals(command.paymentId())));

        // when
        mockMvc.perform(post("/api/v1/internal/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                // then
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ARCPAY-SETTLEMENT-0002"));
    }

    @Test
    void shouldReturnValidationErrorWhenWalletIdBlank() throws Exception {
        // when
        mockMvc.perform(post("/api/v1/internal/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(INVALID_REQUEST))
                // then
                .andExpect(status().isUnprocessableEntity());
        then(transferSubmissionService).shouldHaveNoInteractions();
    }
}
