package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.IllegalReservationStateException;
import com.arcpay.policy.policyengine.domain.exception.ReservationNotFoundException;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.model.ReservationStatus;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.port.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_PAYMENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_RECIPIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("100.000000");
    private static final Instant REQUESTED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SpendingLockService spendingLockService;
    @Mock
    private SpendingLedgerService spendingLedgerService;
    @Mock
    private PolicyEvaluationService policyEvaluationService;
    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    private final AgentInfo agent = AgentInfo.builder()
            .agentId(SOME_AGENT_ID)
            .ownerId(UUID.randomUUID())
            .status("ACTIVE")
            .policyHash("0xhash")
            .build();

    @Test
    void shouldHoldReservationWhenPolicyApproves() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(reservationRepository.sumActiveHeldAmount(SOME_AGENT_ID)).willReturn(BigDecimal.ZERO);
        var approved = resultWith(PolicyVerdict.APPROVED);
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT, false, BigDecimal.ZERO))
                .willReturn(approved);

        // when
        var result = reservationService.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(approved);
        then(spendingLockService).should().acquireLock(SOME_AGENT_ID);
        then(reservationRepository).should().save(reservationCaptor.capture());
        var expectedHeld = Reservation.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .amount(AMOUNT)
                .recipient(SOME_RECIPIENT)
                .status(ReservationStatus.HELD)
                .build();
        assertThat(reservationCaptor.getValue())
                .usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(expectedHeld);
    }

    @Test
    void shouldNotHoldReservationWhenPolicyRejects() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(reservationRepository.sumActiveHeldAmount(SOME_AGENT_ID)).willReturn(BigDecimal.ZERO);
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT, false, BigDecimal.ZERO))
                .willReturn(resultWith(PolicyVerdict.REJECTED));

        // when
        var result = reservationService.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.REJECTED);
        then(reservationRepository).should(never()).save(any());
    }

    @Test
    void shouldReturnApprovedWithoutDoubleHoldingWhenReservationAlreadyHeld() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(heldReservation()));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when
        var result = reservationService.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        then(policyEvaluationService).shouldHaveNoInteractions();
        then(reservationRepository).should(never()).save(any());
        then(reservationRepository).should(never()).sumActiveHeldAmount(any());
    }

    @Test
    void shouldCommitHeldReservationAndPersistCommittedState() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(heldReservation()));
        given(reservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var committed = reservationService.commit(SOME_PAYMENT_ID);

        // then
        then(reservationRepository).should().save(reservationCaptor.capture());
        var expected = Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, AMOUNT, SOME_RECIPIENT, REQUESTED_AT).commit();
        assertThat(reservationCaptor.getValue()).usingRecursiveComparison().isEqualTo(expected);
        assertThat(committed.status()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void shouldBeNoOpWhenCommittingAlreadyCommittedReservation() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(heldReservation().commit()));

        // when
        var committed = reservationService.commit(SOME_PAYMENT_ID);

        // then
        assertThat(committed.status()).isEqualTo(ReservationStatus.COMMITTED);
        then(reservationRepository).should(never()).save(any());
        then(spendingLedgerService).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectCommitOfReleasedReservation() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(heldReservation().release()));

        // when
        // then
        assertThatThrownBy(() -> reservationService.commit(SOME_PAYMENT_ID))
                .isInstanceOf(IllegalReservationStateException.class);
    }

    @Test
    void shouldReleaseHeldReservation() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(heldReservation()));
        given(reservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var released = reservationService.release(SOME_PAYMENT_ID);

        // then
        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void shouldRejectReleaseOfCommittedReservation() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(heldReservation().commit()));

        // when
        // then
        assertThatThrownBy(() -> reservationService.release(SOME_PAYMENT_ID))
                .isInstanceOf(IllegalReservationStateException.class);
    }

    @Test
    void shouldOpsReleaseOrphanHeldReservation() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(heldReservation()));
        given(reservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var released = reservationService.opsRelease(SOME_PAYMENT_ID);

        // then
        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void shouldThrowWhenReservationNotFoundOnCommit() {
        // given
        given(reservationRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reservationService.commit(SOME_PAYMENT_ID))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    private Reservation heldReservation() {
        return Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, AMOUNT, SOME_RECIPIENT, REQUESTED_AT);
    }

    private PolicyEvaluationResult resultWith(PolicyVerdict verdict) {
        return PolicyEvaluationResult.builder()
                .evaluationId(UUID.randomUUID())
                .agentId(SOME_AGENT_ID)
                .policyId(UUID.randomUUID())
                .verdict(verdict)
                .ruleResults(List.of())
                .requestedAmount(AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(false)
                .evaluatedAt(REQUESTED_AT)
                .durationMs(1)
                .build();
    }
}
