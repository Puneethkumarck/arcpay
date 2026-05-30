package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.ReservationResponseMapper;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.spending.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalReservationControllerTest {

    private static final BigDecimal AMOUNT = new BigDecimal("100.000000");
    private static final Instant REQUESTED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private AgentServiceClient agentServiceClient;
    @Mock
    private ReservationService reservationService;

    private InternalReservationController controller;

    private final AgentInfo agent = AgentInfo.builder()
            .agentId(SOME_AGENT_ID)
            .ownerId(UUID.randomUUID())
            .status("ACTIVE")
            .policyHash("0xhash")
            .build();

    @BeforeEach
    void setUp() {
        controller = new InternalReservationController(
                agentServiceClient,
                reservationService,
                Mappers.getMapper(EvaluationResponseMapper.class),
                Mappers.getMapper(ReservationResponseMapper.class));
    }

    @Test
    void shouldReserveAndReturnEvaluationResponse() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(agent));
        given(reservationService.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, agent, SOME_RECIPIENT, AMOUNT, REQUESTED_AT))
                .willReturn(approvedResult());

        // when
        var response = controller.reserve(reserveRequest());

        // then
        assertThat(response.agentId()).isEqualTo(SOME_AGENT_ID);
        assertThat(response.verdict()).isEqualTo("APPROVED");
    }

    @Test
    void shouldThrowAgentNotFoundWhenAgentMissing() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> controller.reserve(reserveRequest()))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldCommitAndReturnReservationStatus() {
        // given
        given(reservationService.commit(SOME_PAYMENT_ID))
                .willReturn(Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, AMOUNT, SOME_RECIPIENT, REQUESTED_AT).commit());

        // when
        var response = controller.commit(SOME_PAYMENT_ID);

        // then
        var expected = ReservationResponse.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .status("COMMITTED")
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReleaseAndReturnReservationStatus() {
        // given
        given(reservationService.release(SOME_PAYMENT_ID))
                .willReturn(Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, AMOUNT, SOME_RECIPIENT, REQUESTED_AT).release());

        // when
        var response = controller.release(SOME_PAYMENT_ID);

        // then
        var expected = ReservationResponse.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .status("RELEASED")
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldOpsReleaseAndReturnReservationStatus() {
        // given
        given(reservationService.opsRelease(SOME_PAYMENT_ID))
                .willReturn(Reservation.held(SOME_PAYMENT_ID, SOME_AGENT_ID, AMOUNT, SOME_RECIPIENT, REQUESTED_AT).release());

        // when
        var response = controller.opsRelease(SOME_PAYMENT_ID);

        // then
        assertThat(response.status()).isEqualTo("RELEASED");
    }

    private ReserveRequest reserveRequest() {
        return ReserveRequest.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(AMOUNT)
                .requestedAt(REQUESTED_AT)
                .build();
    }

    private PolicyEvaluationResult approvedResult() {
        return PolicyEvaluationResult.builder()
                .evaluationId(UUID.randomUUID())
                .agentId(SOME_AGENT_ID)
                .policyId(UUID.randomUUID())
                .verdict(PolicyVerdict.APPROVED)
                .ruleResults(List.of())
                .requestedAmount(AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(false)
                .evaluatedAt(REQUESTED_AT)
                .durationMs(1)
                .build();
    }
}
