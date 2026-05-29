package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.Verdict;
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
@Table(name = "screening_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
class ScreeningResultEntity {

    @Id
    @Column(name = "screening_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID screeningId;

    @Column(name = "payment_id", nullable = false, updatable = false, unique = true)
    private UUID paymentId;

    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Column(name = "recipient_address", nullable = false, updatable = false, length = 64)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, updatable = false, length = 8)
    private Verdict verdict;

    @Column(name = "risk_score", nullable = false, updatable = false)
    private int riskScore;

    @Column(name = "list_version_id", updatable = false)
    private UUID listVersionId;

    @Column(name = "screened_at", nullable = false, updatable = false)
    private Instant screenedAt;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;
}
