package com.arcpay.settlement.application.receipt;

import com.arcpay.settlement.application.receipt.mapper.ReceiptRequestMapper;
import com.arcpay.settlement.domain.model.ReceiptCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.SOME_PAYEE_ADDRESS;
import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.SOME_PAYER_ADDRESS;
import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.SOME_RECEIPT_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.someReceiptCommand;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalReceiptControllerTest {

    private static final String VALID_REQUEST = """
            {
              "paymentId": "%s",
              "payerAgent": "%s",
              "payee": "%s",
              "amount": "12.500000",
              "memo": "invoice-4815"
            }
            """.formatted(SOME_RECEIPT_PAYMENT_ID, SOME_PAYER_ADDRESS, SOME_PAYEE_ADDRESS);

    private static final String INVALID_REQUEST = """
            {
              "paymentId": "%s",
              "payerAgent": " ",
              "payee": "%s",
              "amount": "12.500000"
            }
            """.formatted(SOME_RECEIPT_PAYMENT_ID, SOME_PAYEE_ADDRESS);

    @Mock
    private ReceiptDispatcher receiptDispatcher;

    @Mock
    private ReceiptRequestMapper receiptRequestMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalReceiptController(receiptDispatcher, receiptRequestMapper))
                .build();
    }

    @Test
    void shouldReturnAcceptedAndDispatchReceiptWrite() throws Exception {
        // given
        var command = someReceiptCommand();
        given(receiptRequestMapper.toDomain(org.mockito.ArgumentMatchers.argThat(
                request -> request != null && SOME_RECEIPT_PAYMENT_ID.equals(request.paymentId()))))
                .willReturn(command);

        // when
        mockMvc.perform(post("/api/v1/internal/receipts")
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                // then
                .andExpect(status().isAccepted());
        then(receiptDispatcher).should().dispatch(command);
    }

    @Test
    void shouldReturnBadRequestWhenPayerAgentBlank() throws Exception {
        // when
        mockMvc.perform(post("/api/v1/internal/receipts")
                        .contentType(APPLICATION_JSON)
                        .content(INVALID_REQUEST))
                // then
                .andExpect(status().isBadRequest());
        then(receiptDispatcher).shouldHaveNoInteractions();
    }
}
