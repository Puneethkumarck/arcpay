package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class AgentEntity {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID agentId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "purpose", nullable = false, length = 256)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentStatus status;

    @Column(name = "wallet_id", length = 255)
    private String walletId;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;

    @Column(name = "on_chain_tx_hash", length = 66)
    private String onChainTxHash;

    @Column(name = "policy_hash", length = 66)
    private String policyHash;

    @Column(name = "metadata_hash", nullable = false, length = 66)
    private String metadataHash;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
