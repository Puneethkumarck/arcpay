package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.payment.paymentexecution.domain.exception.SettlementServiceUnavailableException;
import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SettlementServiceAdapterTest {

    private static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    private static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");

    @Mock
    private SettlementServiceClient settlementClient;

    @InjectMocks
    private SettlementServiceAdapter adapter;

    @Test
    void shouldReturnCircleTxIdFromTransfer() {
        // given
        given(settlementClient.submitTransfer(ArgumentMatchers.argThat(
                req -> req.paymentId().equals(SOME_PAYMENT_ID)
                        && req.walletId().equals(SOME_AGENT_ID.toString())
                        && req.recipientAddress().equals(SOME_RECIPIENT)
                        && req.amount().equals(SOME_AMOUNT))))
                .willReturn(TransferStatusResponse.builder().circleTxId("ctx-123").build());

        // when
        var txId = adapter.transfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);

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
    void shouldPostReceiptForPaymentId() {
        // when
        var receipt = adapter.writeReceipt(SOME_PAYMENT_ID);

        // then
        then(settlementClient).should().recordReceipt(
                ArgumentMatchers.argThat(req -> req.paymentId().equals(SOME_PAYMENT_ID)));
        assertThat(receipt.timestamp()).isNotNull();
    }

    @Test
    void shouldMapOpenCircuitToUnavailableOnTransfer() {
        // given
        var openCircuit = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("settlement", CircuitBreakerConfig.ofDefaults()));
        given(settlementClient.submitTransfer(ArgumentMatchers.any()))
                .willThrow(new SettlementServiceCallException("Settlement service call failed", openCircuit));

        // when / then
        assertThatThrownBy(() -> adapter.transfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(SettlementServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open")
                .hasMessageContaining(SOME_PAYMENT_ID.toString());
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
}
