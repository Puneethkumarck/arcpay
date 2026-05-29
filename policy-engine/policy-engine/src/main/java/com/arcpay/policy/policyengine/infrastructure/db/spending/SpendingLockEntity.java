package com.arcpay.policy.policyengine.infrastructure.db.spending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spending_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class SpendingLockEntity {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID agentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
