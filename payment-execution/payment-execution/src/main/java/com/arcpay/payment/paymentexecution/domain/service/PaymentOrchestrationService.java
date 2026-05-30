package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.exception.InvalidPaymentRequestException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentRequest;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.PaymentTransition;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private static final String SUPPORTED_CURRENCY = "USDC";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.000001");
    private static final int MAX_MEMO_LENGTH = 256;
    private static final Pattern RECIPIENT_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final String FINGERPRINT_DELIMITER = "|";

    private final PaymentStateMachine paymentStateMachine;

    public Payment newPayment(PaymentRequest request) {
        validate(request);
        var now = Instant.now();
        return Payment.builder()
                .paymentId(UuidCreator.getTimeOrderedEpoch())
                .agentId(request.agentId())
                .ownerId(request.ownerId())
                .idempotencyKey(request.idempotencyKey())
                .requestFingerprint(fingerprint(request))
                .recipientAddress(request.recipientAddress())
                .amount(request.amount())
                .currency(request.currency())
                .memo(request.memo())
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public String fingerprint(PaymentRequest request) {
        var canonical = String.join(FINGERPRINT_DELIMITER,
                request.agentId().toString(),
                request.recipientAddress(),
                request.amount().stripTrailingZeros().toPlainString(),
                request.currency(),
                request.memo() == null ? "" : request.memo());
        return Hash.sha3String(canonical);
    }

    public PaymentTransition transition(Payment payment, PaymentStatus toStatus, Instant transitionedAt) {
        return paymentStateMachine.transition(payment, toStatus, transitionedAt);
    }

    public PaymentTransition reject(Payment payment, RejectionReason reason, Instant transitionedAt) {
        return paymentStateMachine.reject(payment, reason, transitionedAt);
    }

    public PaymentTransition fail(Payment payment, FailureReason reason, Instant transitionedAt) {
        return paymentStateMachine.fail(payment, reason, transitionedAt);
    }

    public PaymentTransition complete(Payment payment, Instant transitionedAt) {
        return paymentStateMachine.complete(payment, transitionedAt);
    }

    private void validate(PaymentRequest request) {
        if (!SUPPORTED_CURRENCY.equals(request.currency())) {
            throw new InvalidPaymentRequestException("Unsupported currency: " + request.currency());
        }
        if (!RECIPIENT_PATTERN.matcher(request.recipientAddress()).matches()) {
            throw new InvalidPaymentRequestException("Invalid recipient address: " + request.recipientAddress());
        }
        if (request.amount().compareTo(MIN_AMOUNT) < 0) {
            throw new InvalidPaymentRequestException("Amount below minimum: " + request.amount().toPlainString());
        }
        if (request.memo() != null && request.memo().length() > MAX_MEMO_LENGTH) {
            throw new InvalidPaymentRequestException("Memo exceeds maximum length of " + MAX_MEMO_LENGTH);
        }
    }
}
