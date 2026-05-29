package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.ReviewState;
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
@Table(name = "hold_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
class HoldReviewEntity {

    @Id
    @Column(name = "review_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID reviewId;

    @Column(name = "screening_id", nullable = false, updatable = false)
    private UUID screeningId;

    @Column(name = "payment_id", nullable = false, updatable = false, unique = true)
    private UUID paymentId;

    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private ReviewState state;

    @Column(name = "reviewer_principal", length = 255)
    private String reviewerPrincipal;

    @Column(name = "reviewer_role", length = 32)
    private String reviewerRole;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
