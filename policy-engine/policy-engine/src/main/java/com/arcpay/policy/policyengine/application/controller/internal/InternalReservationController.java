package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.ReservationResponseMapper;
import com.arcpay.policy.policyengine.application.controller.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.spending.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/policies/reservations")
@RequiredArgsConstructor
@Validated
public class InternalReservationController {

    private final AgentServiceClient agentServiceClient;
    private final ReservationService reservationService;
    private final EvaluationResponseMapper evaluationResponseMapper;
    private final ReservationResponseMapper reservationResponseMapper;

    @PostMapping
    public PolicyEvaluationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        log.info("Reservation requested paymentId={} agentId={} amount={}",
                request.paymentId(), request.agentId(), request.amount());
        var agent = agentServiceClient.getAgent(request.agentId())
                .orElseThrow(() -> new AgentNotFoundException(request.agentId()));
        var result = reservationService.reserve(
                request.paymentId(),
                request.agentId(),
                agent,
                request.recipientAddress(),
                request.amount(),
                request.requestedAt());
        return evaluationResponseMapper.toApi(result);
    }

    @PostMapping("/{paymentId}/commit")
    public ReservationResponse commit(@PathVariable UUID paymentId) {
        log.info("Reservation commit requested paymentId={}", paymentId);
        return reservationResponseMapper.toApi(reservationService.commit(paymentId));
    }

    @PostMapping("/{paymentId}/release")
    public ReservationResponse release(@PathVariable UUID paymentId) {
        log.info("Reservation release requested paymentId={}", paymentId);
        return reservationResponseMapper.toApi(reservationService.release(paymentId));
    }

    @PostMapping("/{paymentId}/ops-release")
    public ReservationResponse opsRelease(@PathVariable UUID paymentId) {
        log.info("Reservation ops-release requested paymentId={}", paymentId);
        return reservationResponseMapper.toApi(reservationService.opsRelease(paymentId));
    }
}
