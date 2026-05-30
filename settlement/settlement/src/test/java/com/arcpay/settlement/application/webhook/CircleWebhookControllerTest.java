package com.arcpay.settlement.application.webhook;

import com.arcpay.settlement.application.controller.GlobalExceptionHandler;
import com.arcpay.settlement.domain.TransferNotificationHandler;
import com.arcpay.settlement.domain.WebhookSignatureException;
import com.arcpay.settlement.domain.model.TransferNotification;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.SOME_KEY_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.notificationBody;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CircleWebhookControllerTest {

    private static final String VALID_SIGNATURE = "valid-signature";
    private static final String BODY = notificationBody(SOME_CIRCLE_TX_ID, "COMPLETE");

    @Mock
    private WebhookSignatureVerifier signatureVerifier;

    @Mock
    private CircleNotificationParser notificationParser;

    @Mock
    private TransferNotificationHandler notificationHandler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CircleWebhookController(
                        signatureVerifier, notificationParser, notificationHandler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldVerifySignatureParseAndHandleAndReturnOk() throws Exception {
        // given
        var notification = TransferNotification.builder()
                .circleTxId(SOME_CIRCLE_TX_ID)
                .state(TransferState.COMPLETED)
                .build();
        given(notificationParser.parse(BODY)).willReturn(notification);

        // when
        mockMvc.perform(post("/api/v1/webhooks/circle")
                        .header("X-Circle-Key-Id", SOME_KEY_ID)
                        .header("X-Circle-Signature", VALID_SIGNATURE)
                        .contentType(APPLICATION_JSON)
                        .content(BODY))
                // then
                .andExpect(status().isOk());
        then(signatureVerifier).should().verify(BODY, SOME_KEY_ID, VALID_SIGNATURE);
        then(notificationHandler).should().handle(notification);
    }

    @Test
    void shouldReturnUnauthorizedAndNotHandleWhenSignatureInvalid() throws Exception {
        // given
        doThrow(new WebhookSignatureException("bad signature"))
                .when(signatureVerifier).verify(BODY, SOME_KEY_ID, "bad");

        // when
        mockMvc.perform(post("/api/v1/webhooks/circle")
                        .header("X-Circle-Key-Id", SOME_KEY_ID)
                        .header("X-Circle-Signature", "bad")
                        .contentType(APPLICATION_JSON)
                        .content(BODY))
                // then
                .andExpect(status().isUnauthorized());
        then(notificationParser).shouldHaveNoInteractions();
        then(notificationHandler).shouldHaveNoInteractions();
    }
}
