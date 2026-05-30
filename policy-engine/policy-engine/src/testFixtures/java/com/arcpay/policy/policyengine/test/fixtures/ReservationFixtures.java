package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AMOUNT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_PAYMENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_RECIPIENT;

public final class ReservationFixtures {

    private ReservationFixtures() {}

    public static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000020");
    public static final UUID SOME_EVALUATION_ID = UUID.fromString("019576a0-0000-7000-8000-000000000021");
    public static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000022");
    public static final String SOME_POLICY_HASH = "0xhash";
    public static final Instant SOME_REQUESTED_AT = Instant.parse("2026-01-01T12:00:00Z");

    public static final AgentInfo SOME_AGENT =
            new AgentInfo(SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", SOME_POLICY_HASH);

    public static final Reservation SOME_HELD_RESERVATION =
            Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_AMOUNT, SOME_RECIPIENT, SOME_REQUESTED_AT);

    public static final ReserveRequest SOME_RESERVE_REQUEST =
            reserveRequest(SOME_AGENT_ID, SOME_PAYMENT_ID, SOME_AMOUNT);

    public static PolicyEvaluationResult evaluationResult(PolicyVerdict verdict) {
        return PolicyEvaluationResult.builder()
                .evaluationId(SOME_EVALUATION_ID)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(verdict)
                .ruleResults(List.of())
                .requestedAmount(SOME_AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(false)
                .evaluatedAt(SOME_REQUESTED_AT)
                .durationMs(1)
                .build();
    }

    public static ReserveRequest reserveRequest(UUID agentId, UUID paymentId, BigDecimal amount) {
        return ReserveRequest.builder()
                .paymentId(paymentId)
                .agentId(agentId)
                .recipientAddress(SOME_RECIPIENT)
                .amount(amount)
                .requestedAt(SOME_REQUESTED_AT)
                .build();
    }
}
