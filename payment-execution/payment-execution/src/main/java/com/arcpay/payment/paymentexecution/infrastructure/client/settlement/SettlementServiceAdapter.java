package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.payment.paymentexecution.api.model.PaymentReceipt;
import com.arcpay.payment.paymentexecution.domain.exception.SettlementServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.port.SettlementPort;
import com.arcpay.settlement.api.model.ReceiptRequest;
import com.arcpay.settlement.api.model.TransferRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
class SettlementServiceAdapter implements SettlementPort {

    private final SettlementServiceClient settlementClient;

    @Override
    public String transfer(UUID paymentId, String walletId, String recipientAddress, BigDecimal amount) {
        try {
            var request = TransferRequest.builder()
                    .paymentId(paymentId)
                    .walletId(walletId)
                    .recipientAddress(recipientAddress)
                    .amount(amount)
                    .build();
            var response = settlementClient.submitTransfer(request);
            return response.circleTxId();
        } catch (SettlementServiceCallException e) {
            throw toUnavailable("transfer", paymentId, e);
        }
    }

    @Override
    public BigDecimal balance(UUID agentId) {
        try {
            return settlementClient.balance(agentId.toString()).amount();
        } catch (SettlementServiceCallException e) {
            throw toUnavailable("balance", agentId, e);
        }
    }

    @Override
    public PaymentReceipt writeReceipt(Payment payment) {
        try {
            settlementClient.recordReceipt(ReceiptRequest.builder()
                    .paymentId(payment.paymentId())
                    .payerAgent(payment.agentId().toString())
                    .payee(payment.recipientAddress())
                    .amount(payment.amount())
                    .memo(payment.memo())
                    .build());
            return PaymentReceipt.builder().build();
        } catch (SettlementServiceCallException e) {
            throw toUnavailable("writeReceipt", payment.paymentId(), e);
        }
    }

    private SettlementServiceUnavailableException toUnavailable(String operation, UUID id, SettlementServiceCallException e) {
        var reason = switch (e.getCause()) {
            case CallNotPermittedException _ -> "Settlement service circuit breaker is open";
            case TimeoutException _ -> "Settlement service call timed out";
            case null, default -> "Settlement service call failed";
        };
        return new SettlementServiceUnavailableException(
                reason + " during " + operation + " for " + id, e);
    }
}
