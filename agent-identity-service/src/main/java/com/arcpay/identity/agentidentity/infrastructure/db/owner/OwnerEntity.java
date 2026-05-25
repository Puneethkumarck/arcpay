package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class OwnerEntity {

    @Id
    @Column(name = "owner_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID ownerId;

    @Column(name = "email", nullable = false, updatable = false, length = 255)
    private String email;

    @Column(name = "wallet_address", nullable = false, updatable = false, length = 42)
    private String walletAddress;

    @Column(name = "api_key_hash", nullable = false, updatable = false, length = 64)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OwnerStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
