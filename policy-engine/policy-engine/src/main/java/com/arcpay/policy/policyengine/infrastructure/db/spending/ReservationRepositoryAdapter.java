package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        var saved = jpaRepository.save(mapToEntity(reservation));
        return mapToDomain(saved);
    }

    @Override
    public Optional<Reservation> findByPaymentId(UUID paymentId) {
        return jpaRepository.findById(paymentId).map(this::mapToDomain);
    }

    @Override
    public BigDecimal sumActiveHeldAmount(UUID agentId) {
        return jpaRepository.sumActiveHeldAmount(agentId);
    }

    private ReservationEntity mapToEntity(Reservation reservation) {
        return ReservationEntity.builder()
                .paymentId(reservation.paymentId())
                .agentId(reservation.agentId())
                .amount(reservation.amount())
                .recipient(reservation.recipient())
                .status(reservation.status())
                .createdAt(reservation.createdAt())
                .build();
    }

    private Reservation mapToDomain(ReservationEntity entity) {
        return Reservation.builder()
                .paymentId(entity.getPaymentId())
                .agentId(entity.getAgentId())
                .amount(entity.getAmount())
                .recipient(entity.getRecipient())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
