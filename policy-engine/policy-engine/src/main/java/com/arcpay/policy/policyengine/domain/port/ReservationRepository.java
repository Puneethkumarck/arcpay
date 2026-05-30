package com.arcpay.policy.policyengine.domain.port;

import com.arcpay.policy.policyengine.domain.model.Reservation;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findByPaymentId(UUID paymentId);

    BigDecimal sumActiveHeldAmount(UUID agentId);
}
