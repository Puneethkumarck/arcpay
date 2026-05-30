package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.ReservationResponseMapper;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.spending.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.ReservationFixtures.SOME_AGENT;
import static com.arcpay.policy.policyengine.test.fixtures.ReservationFixtures.SOME_HELD_RESERVATION;
import static com.arcpay.policy.policyengine.test.fixtures.ReservationFixtures.SOME_RESERVE_REQUEST;
import static com.arcpay.policy.policyengine.test.fixtures.ReservationFixtures.evaluationResult;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AMOUNT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_PAYMENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_RECIPIENT;
import static com.arcpay.policy.policyengine.test.fixtures.ReservationFixtures.SOME_REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalReservationControllerTest {

    @Mock
    private AgentServiceClient agentServiceClient;
    @Mock
    private ReservationService reservationService;

    private InternalReservationController controller;

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
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_AGENT));
        given(reservationService.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_AGENT, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT))
                .willReturn(evaluationResult(PolicyVerdict.APPROVED));

        // when
        var response = controller.reserve(SOME_RESERVE_REQUEST);

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
        assertThatThrownBy(() -> controller.reserve(SOME_RESERVE_REQUEST))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldCommitAndReturnReservationStatus() {
        // given
        given(reservationService.commit(SOME_PAYMENT_ID)).willReturn(SOME_HELD_RESERVATION.commit());

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
        given(reservationService.release(SOME_PAYMENT_ID)).willReturn(SOME_HELD_RESERVATION.release());

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
        given(reservationService.opsRelease(SOME_PAYMENT_ID)).willReturn(SOME_HELD_RESERVATION.release());

        // when
        var response = controller.opsRelease(SOME_PAYMENT_ID);

        // then
        var expected = ReservationResponse.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .status("RELEASED")
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
