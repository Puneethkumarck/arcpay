package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AMOUNT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_CURRENCY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CompliancePortAdapterTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CompliancePortAdapter compliancePortAdapter;

    @Test
    void shouldPublishScreeningRequestBuiltFromPayment() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);

        var expectedEvent = PaymentScreeningRequested.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .requestedAt(Instant.now())
                .build();

        // when
        compliancePortAdapter.publishScreeningRequest(payment);

        // then
        then(eventPublisher).should().publish(eqIgnoringTimestamps(expectedEvent));
    }
}
