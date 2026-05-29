package com.arcpay.policy.policyengine.infrastructure.db.evaluation;

import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class PolicyEvaluationEntity {

    @Id
    @Column(name = "evaluation_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID evaluationId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20)
    private PolicyVerdict verdict;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_results", columnDefinition = "jsonb", nullable = false)
    private String ruleResults;

    @Column(name = "requested_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal requestedAmount;

    @Column(name = "recipient_address", nullable = false, length = 42)
    private String recipientAddress;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;
}
