package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.payment.paymentexecution.api.model.PaymentReceipt;
import com.arcpay.payment.paymentexecution.domain.exception.SettlementServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.ReceiptRequest;
import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SettlementServiceAdapterTest {

    private static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    private static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    private static final UUID SOME_OWNER_ID = UUID.fromString("0197aa00-3333-7def-8000-333333333333");
    private static final String SOME_WALLET_ID = "circle-wallet-abc";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");

    private static final Payment SOME_PAYMENT = Payment.builder()
            .paymentId(SOME_PAYMENT_ID)
            .agentId(SOME_AGENT_ID)
            .ownerId(SOME_OWNER_ID)
            .idempotencyKey("idem-1")
            .requestFingerprint("fingerprint-1")
            .recipientAddress(SOME_RECIPIENT)
            .amount(SOME_AMOUNT)
            .currency("USDC")
            .memo("invoice-42")
            .status(PaymentStatus.EXECUTING)
            .createdAt(Instant.parse("2026-05-30T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-30T10:00:00Z"))
            .build();

    @Mock
    private SettlementServiceClient settlementClient;

    @InjectMocks
    private SettlementServiceAdapter adapter;

    @Test
    void shouldReturnCircleTxIdFromTransfer() {
        // given
        var expectedRequest = TransferRequest.builder()
                .paymentId(SOME_PAYMENT_ID)
                .walletId(SOME_WALLET_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .build();
        given(settlementClient.submitTransfer(eqIgnoring(expectedRequest)))
                .willReturn(TransferStatusResponse.builder().circleTxId("ctx-123").build());

        // when
        var txId = adapter.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT);

        // then
        assertThat(txId).isEqualTo("ctx-123");
    }

    @Test
    void shouldReturnBalanceAmount() {
        // given
        given(settlementClient.balance(SOME_AGENT_ID.toString()))
                .willReturn(BalanceResponse.builder().amount(new BigDecimal("100.00")).build());

        // when
        var balance = adapter.balance(SOME_AGENT_ID);

        // then
        assertThat(balance).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldPostFullReceiptRequestBody() {
        // given
        var expectedRequest = ReceiptRequest.builder()
                .paymentId(SOME_PAYMENT_ID)
                .payerAgent(SOME_AGENT_ID.toString())
                .payee(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .memo("invoice-42")
                .build();

        // when
        var receipt = adapter.writeReceipt(SOME_PAYMENT);

        // then
        then(settlementClient).should().recordReceipt(eqIgnoring(expectedRequest));
        assertThat(receipt).usingRecursiveComparison().isEqualTo(PaymentReceipt.builder().build());
    }

    @Test
    void shouldMapOpenCircuitToUnavailableOnTransfer() {
        // given
        var openCircuit = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("settlement", CircuitBreakerConfig.ofDefaults()));
        var expectedRequest = TransferRequest.builder()
                .paymentId(SOME_PAYMENT_ID)
                .walletId(SOME_WALLET_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .build();
        given(settlementClient.submitTransfer(eqIgnoring(expectedRequest)))
                .willThrow(new SettlementServiceCallException("Settlement service call failed", openCircuit));

        // when / then
        assertThatThrownBy(() -> adapter.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(SettlementServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open")
                .hasMessageContaining(SOME_PAYMENT_ID.toString());
    }

    @Test
    void shouldPropagateClientErrorWithoutTreatingAsUnavailableOnTransfer() {
        // given
        var expectedRequest = TransferRequest.builder()
                .paymentId(SOME_PAYMENT_ID)
                .walletId(SOME_WALLET_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .build();
        given(settlementClient.submitTransfer(eqIgnoring(expectedRequest)))
                .willThrow(unprocessableEntity());

        // when / then
        assertThatThrownBy(() -> adapter.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(FeignException.UnprocessableEntity.class)
                .isNotInstanceOf(SettlementServiceUnavailableException.class);
    }

    @Test
    void shouldMapCallFailureToUnavailableOnBalance() {
        // given
        given(settlementClient.balance(SOME_AGENT_ID.toString()))
                .willThrow(new SettlementServiceCallException("Settlement service call failed", new RuntimeException("boom")));

        // when / then
        assertThatThrownBy(() -> adapter.balance(SOME_AGENT_ID))
                .isInstanceOf(SettlementServiceUnavailableException.class)
                .hasMessageContaining("during balance");
    }

    private FeignException.UnprocessableEntity unprocessableEntity() {
        var request = Request.create(Request.HttpMethod.POST, "/api/v1/internal/transfers",
                Map.of(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.UnprocessableEntity("insufficient balance", request, new byte[0], Map.of());
    }
}
