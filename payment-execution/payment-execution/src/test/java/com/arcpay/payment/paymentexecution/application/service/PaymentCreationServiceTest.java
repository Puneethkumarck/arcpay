package com.arcpay.payment.paymentexecution.application.service;

import com.arcpay.payment.paymentexecution.domain.agent.AgentAuthorization;
import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotActiveException;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.exception.IdempotencyConflictException;
import com.arcpay.payment.paymentexecution.domain.exception.InvalidPaymentRequestException;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotOwnedException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.domain.service.PaymentOrchestrationService;
import com.arcpay.platform.api.OwnerPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someAgentInfo;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someCreatePaymentRequest;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentCreationServiceTest {

    @Mock
    private AgentAuthorization agentAuthorization;

    @Mock
    private PaymentOrchestrationService paymentOrchestrationService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentCreationService paymentCreationService;

    private static OwnerPrincipal owner() {
        return new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
    }

    @Test
    void shouldCreatePaymentAndPublishEvent() {
        // given
        var request = someCreatePaymentRequest();
        var pending = somePayment(PaymentStatus.PENDING);
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willReturn(someAgentInfo("ACTIVE"));
        given(paymentOrchestrationService.newPayment(eqIgnoringTimestamps(
                com.arcpay.payment.paymentexecution.domain.model.PaymentRequest.builder()
                        .agentId(SOME_AGENT_ID)
                        .ownerId(SOME_OWNER_ID)
                        .idempotencyKey(request.idempotencyKey())
                        .recipientAddress(request.recipientAddress())
                        .amount(request.amount())
                        .currency(request.currency())
                        .memo(request.memo())
                        .metadata(request.metadata())
                        .build())))
                .willReturn(pending);
        given(paymentRepository.save(pending)).willReturn(pending);

        // when
        var result = paymentCreationService.create(owner(), request);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(new PaymentCreationResult(pending, true));
        then(eventPublisher).should().publish(eqIgnoringTimestamps(expectedEvent(pending)));
    }

    @Test
    void shouldReturnReplayWithoutPublishingWhenIdempotentReplay() {
        // given
        var request = someCreatePaymentRequest();
        var pending = somePayment(PaymentStatus.PENDING).toBuilder().paymentId(UUID.randomUUID()).build();
        var existing = somePayment(PaymentStatus.SCREENING);
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willReturn(someAgentInfo("ACTIVE"));
        given(paymentOrchestrationService.newPayment(eqIgnoringTimestamps(
                com.arcpay.payment.paymentexecution.domain.model.PaymentRequest.builder()
                        .agentId(SOME_AGENT_ID)
                        .ownerId(SOME_OWNER_ID)
                        .idempotencyKey(request.idempotencyKey())
                        .recipientAddress(request.recipientAddress())
                        .amount(request.amount())
                        .currency(request.currency())
                        .memo(request.memo())
                        .metadata(request.metadata())
                        .build())))
                .willReturn(pending);
        given(paymentRepository.save(pending)).willReturn(existing);

        // when
        var result = paymentCreationService.create(owner(), request);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(new PaymentCreationResult(existing, false));
        then(eventPublisher).should(never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPropagateIdempotencyConflict() {
        // given
        var request = someCreatePaymentRequest();
        var pending = somePayment(PaymentStatus.PENDING);
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willReturn(someAgentInfo("ACTIVE"));
        given(paymentOrchestrationService.newPayment(org.mockito.ArgumentMatchers.any())).willReturn(pending);
        given(paymentRepository.save(pending)).willThrow(new IdempotencyConflictException("conflict"));

        // when / then
        assertThatThrownBy(() -> paymentCreationService.create(owner(), request))
                .isInstanceOf(IdempotencyConflictException.class);
        then(eventPublisher).should(never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectWhenAgentNotFound() {
        // given
        var request = someCreatePaymentRequest();
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willThrow(new AgentNotFoundException(SOME_AGENT_ID));

        // when / then
        assertThatThrownBy(() -> paymentCreationService.create(owner(), request))
                .isInstanceOf(AgentNotFoundException.class);
        then(paymentRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectWhenAgentNotActive() {
        // given
        var request = someCreatePaymentRequest();
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willThrow(new AgentNotActiveException(SOME_AGENT_ID, "SUSPENDED"));

        // when / then
        assertThatThrownBy(() -> paymentCreationService.create(owner(), request))
                .isInstanceOf(AgentNotActiveException.class);
        then(paymentRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectWhenAgentNotOwned() {
        // given
        var request = someCreatePaymentRequest();
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willThrow(new AgentNotOwnedException(SOME_AGENT_ID, SOME_OWNER_ID));

        // when / then
        assertThatThrownBy(() -> paymentCreationService.create(owner(), request))
                .isInstanceOf(AgentNotOwnedException.class);
    }

    @Test
    void shouldRejectSelfPayment() {
        // given
        var request = com.arcpay.payment.paymentexecution.api.model.CreatePaymentRequest.builder()
                .agentId(SOME_AGENT_ID)
                .idempotencyKey(someCreatePaymentRequest().idempotencyKey())
                .recipientAddress(SOME_WALLET_ADDRESS)
                .amount(someCreatePaymentRequest().amount())
                .currency(someCreatePaymentRequest().currency())
                .memo(someCreatePaymentRequest().memo())
                .metadata(someCreatePaymentRequest().metadata())
                .build();
        given(agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .willReturn(someAgentInfo("ACTIVE"));

        // when / then
        assertThatThrownBy(() -> paymentCreationService.create(owner(), request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining(SOME_WALLET_ADDRESS);
        then(paymentRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static PaymentRequested expectedEvent(Payment payment) {
        return PaymentRequested.builder()
                .paymentId(payment.paymentId())
                .agentId(payment.agentId())
                .ownerId(payment.ownerId())
                .walletId(SOME_WALLET_ID)
                .idempotencyKey(payment.idempotencyKey())
                .recipientAddress(payment.recipientAddress())
                .amount(payment.amount())
                .currency(payment.currency())
                .memo(payment.memo())
                .metadata(payment.metadata())
                .requestedAt(payment.createdAt())
                .build();
    }
}
