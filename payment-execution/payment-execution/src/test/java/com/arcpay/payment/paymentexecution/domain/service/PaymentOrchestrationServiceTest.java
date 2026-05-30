package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.exception.InvalidPaymentRequestException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentRequest;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.PaymentTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Hash;

import java.util.Locale;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AMOUNT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_CURRENCY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_MEMO;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_METADATA;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TRANSITIONED_AT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceTest {

    @Mock
    private PaymentStateMachine paymentStateMachine;

    @InjectMocks
    private PaymentOrchestrationService service;

    @Test
    void shouldComputeDeterministicFingerprint() {
        // given
        var request = someRequest();

        // when
        var first = service.fingerprint(request);
        var second = service.fingerprint(request);

        // then
        var expected = Hash.sha3String(String.join("|",
                SOME_AGENT_ID.toString(), SOME_RECIPIENT, "25", SOME_CURRENCY, SOME_MEMO));
        assertThat(first).isEqualTo(expected);
        assertThat(second).isEqualTo(expected);
    }

    @Test
    void shouldComputeSameFingerprintForEquivalentAmountScale() {
        // given
        var twentyFive = someRequest().toBuilder().amount(new java.math.BigDecimal("25")).build();
        var twentyFiveDecimals = someRequest().toBuilder().amount(new java.math.BigDecimal("25.00")).build();

        // when
        var fingerprintOne = service.fingerprint(twentyFive);
        var fingerprintTwo = service.fingerprint(twentyFiveDecimals);

        // then
        assertThat(fingerprintOne).isEqualTo(fingerprintTwo);
    }

    @Test
    void shouldComputeDifferentFingerprintForDifferentRecipient() {
        // given
        var request = someRequest();
        var other = request.toBuilder()
                .recipientAddress("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd")
                .build();

        // when
        var fingerprint = service.fingerprint(request);
        var otherFingerprint = service.fingerprint(other);

        // then
        assertThat(fingerprint).isNotEqualTo(otherFingerprint);
    }

    @Test
    void shouldComputeSameFingerprintRegardlessOfRecipientCase() {
        // given
        var lowerCase = someRequest();
        var upperCase = someRequest().toBuilder()
                .recipientAddress(SOME_RECIPIENT.toUpperCase(Locale.ROOT))
                .build();

        // when
        var lowerFingerprint = service.fingerprint(lowerCase);
        var upperFingerprint = service.fingerprint(upperCase);

        // then
        assertThat(lowerFingerprint).isEqualTo(upperFingerprint);
    }

    @Test
    void shouldBuildPendingPaymentFromRequest() {
        // given
        var request = someRequest();

        // when
        var payment = service.newPayment(request);

        // then
        var expected = Payment.builder()
                .paymentId(payment.paymentId())
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .idempotencyKey(SOME_IDEMPOTENCY_KEY)
                .requestFingerprint(service.fingerprint(request))
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .memo(SOME_MEMO)
                .metadata(SOME_METADATA)
                .status(PaymentStatus.PENDING)
                .createdAt(payment.createdAt())
                .updatedAt(payment.updatedAt())
                .build();
        assertThat(payment).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAssignTimeOrderedPaymentId() {
        // given
        var request = someRequest();

        // when
        var payment = service.newPayment(request);

        // then
        assertThat(payment.paymentId().version()).isEqualTo(7);
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        // given
        var request = someRequest().toBuilder().currency("EUR").build();

        // when then
        assertThatThrownBy(() -> service.newPayment(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Unsupported currency: EUR");
    }

    @Test
    void shouldRejectInvalidRecipientAddress() {
        // given
        var request = someRequest().toBuilder().recipientAddress("0xnot-an-address").build();

        // when then
        assertThatThrownBy(() -> service.newPayment(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Invalid recipient address: 0xnot-an-address");
    }

    @Test
    void shouldRejectAmountBelowMinimum() {
        // given
        var request = someRequest().toBuilder().amount(new java.math.BigDecimal("0.0000001")).build();

        // when then
        assertThatThrownBy(() -> service.newPayment(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Amount below minimum: 0.0000001");
    }

    @Test
    void shouldRejectMemoExceedingMaxLength() {
        // given
        var request = someRequest().toBuilder().memo("x".repeat(257)).build();

        // when then
        assertThatThrownBy(() -> service.newPayment(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Memo exceeds maximum length of 256");
    }

    @Test
    void shouldDelegateTransitionToStateMachine() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        var transitioned = payment.withStatus(PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT);
        var expected = PaymentTransition.builder()
                .payment(transitioned)
                .event(eventFor(payment, transitioned))
                .build();
        given(paymentStateMachine.transition(payment, PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT))
                .willReturn(expected);

        // when
        var result = service.transition(payment, PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(paymentStateMachine).should().transition(payment, PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT);
    }

    private com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged eventFor(Payment from, Payment to) {
        return com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged.builder()
                .paymentId(to.paymentId())
                .agentId(to.agentId())
                .status(to.status().name())
                .previousStatus(from.status().name())
                .changedAt(to.updatedAt())
                .build();
    }
}
