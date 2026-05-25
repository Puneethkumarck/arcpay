package com.arcpay.identity.agentidentity.infrastructure.db.gasusage;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gas_usage")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class GasUsageEntity {

    @Id
    @Column(nullable = false, updatable = false)
    @ToString.Include
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "agent_id", updatable = false)
    private UUID agentId;

    @Column(nullable = false, updatable = false, length = 50)
    private String operation;

    @Column(name = "tx_hash", nullable = false, updatable = false, length = 66)
    private String txHash;

    @Column(name = "gas_used", nullable = false, updatable = false)
    private long gasUsed;

    @Column(name = "gas_cost_usdc", nullable = false, updatable = false, precision = 18, scale = 8)
    private BigDecimal gasCostUsdc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
