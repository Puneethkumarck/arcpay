package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spending_reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class ReservationEntity {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID paymentId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal amount;

    @Column(name = "recipient", nullable = false, length = 42)
    private String recipient;

    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
