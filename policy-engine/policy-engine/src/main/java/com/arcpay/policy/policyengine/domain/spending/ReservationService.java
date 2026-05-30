package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.IllegalReservationStateException;
import com.arcpay.policy.policyengine.domain.exception.ReservationNotFoundException;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.model.ReservationStatus;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.port.ReservationRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final UUID NO_POLICY_ID = new UUID(0L, 0L);

    private final ReservationRepository reservationRepository;
    private final SpendingLockService spendingLockService;
    private final SpendingLedgerService spendingLedgerService;
    private final PolicyEvaluationService policyEvaluationService;
    private final PolicyRepository policyRepository;

    @Transactional
    public PolicyEvaluationResult reserve(UUID paymentId, UUID agentId, AgentInfo agent,
            String recipientAddress, BigDecimal amount, Instant requestedAt) {
        spendingLockService.acquireLock(agentId);

        var existing = reservationRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.info("Reservation already exists for paymentId={} status={}, returning existing decision",
                    paymentId, existing.get().status());
            return idempotentResult(existing.get());
        }

        var heldTotal = reservationRepository.sumActiveHeldAmount(agentId);
        var result = policyEvaluationService.evaluate(
                agentId, agent, recipientAddress, amount, requestedAt, false, heldTotal);

        if (result.verdict() == PolicyVerdict.APPROVED) {
            reservationRepository.save(Reservation.held(paymentId, agentId, amount, recipientAddress, Instant.now()));
            log.info("Held reservation paymentId={} agentId={} amount={}", paymentId, agentId, amount);
        }
        return result;
    }

    @Transactional
    public Reservation commit(UUID paymentId) {
        var reservation = lockAndLoad(paymentId);
        if (reservation.status() == ReservationStatus.COMMITTED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.RELEASED) {
            throw new IllegalReservationStateException(paymentId, reservation.status(), "commit");
        }

        var committed = reservationRepository.save(reservation.commit());
        spendingLedgerService.recordSpending(committed.agentId(), committed.paymentId(),
                committed.amount(), committed.recipient(), Instant.now());
        log.info("Committed reservation paymentId={} agentId={}", paymentId, committed.agentId());
        return committed;
    }

    @Transactional
    public Reservation release(UUID paymentId) {
        return releaseHeld(paymentId, "release");
    }

    @Transactional
    public Reservation opsRelease(UUID paymentId) {
        log.warn("Operational orphan release requested for paymentId={}", paymentId);
        return releaseHeld(paymentId, "ops-release");
    }

    private Reservation releaseHeld(UUID paymentId, String operation) {
        var reservation = lockAndLoad(paymentId);
        if (reservation.status() == ReservationStatus.RELEASED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.COMMITTED) {
            throw new IllegalReservationStateException(paymentId, reservation.status(), operation);
        }
        var released = reservationRepository.save(reservation.release());
        log.info("Released reservation paymentId={} agentId={} via {}", paymentId, released.agentId(), operation);
        return released;
    }

    private Reservation lockAndLoad(UUID paymentId) {
        var reservation = load(paymentId);
        spendingLockService.acquireLock(reservation.agentId());
        return load(paymentId);
    }

    private Reservation load(UUID paymentId) {
        return reservationRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ReservationNotFoundException(paymentId));
    }

    private PolicyEvaluationResult idempotentResult(Reservation reservation) {
        var verdict = reservation.status() == ReservationStatus.RELEASED
                ? PolicyVerdict.REJECTED
                : PolicyVerdict.APPROVED;
        var policyId = policyRepository.findActiveByAgentId(reservation.agentId())
                .map(Policy::policyId)
                .orElse(NO_POLICY_ID);
        return PolicyEvaluationResult.builder()
                .evaluationId(UuidCreator.getTimeOrderedEpoch())
                .agentId(reservation.agentId())
                .policyId(policyId)
                .verdict(verdict)
                .ruleResults(List.of())
                .requestedAmount(reservation.amount())
                .recipientAddress(reservation.recipient())
                .dryRun(false)
                .evaluatedAt(Instant.now())
                .durationMs(0)
                .build();
    }
}
