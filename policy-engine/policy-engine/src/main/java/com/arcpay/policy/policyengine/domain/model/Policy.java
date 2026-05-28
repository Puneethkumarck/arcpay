package com.arcpay.policy.policyengine.domain.model;

import com.arcpay.policy.policyengine.api.PolicyRule;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record Policy(
        UUID policyId,
        UUID agentId,
        UUID ownerId,
        int version,
        List<PolicyRule> rules,
        String policyHash,
        PolicyStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public Policy {
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(policyHash, "policyHash must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        rules = List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }
    }

    public Policy supersede() {
        return toBuilder()
                .status(PolicyStatus.SUPERSEDED)
                .updatedAt(Instant.now())
                .build();
    }
}
