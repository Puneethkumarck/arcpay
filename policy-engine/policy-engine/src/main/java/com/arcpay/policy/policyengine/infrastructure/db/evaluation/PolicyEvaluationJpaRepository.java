package com.arcpay.policy.policyengine.infrastructure.db.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

interface PolicyEvaluationJpaRepository extends JpaRepository<PolicyEvaluationEntity, UUID> {

    void deleteByEvaluatedAtBefore(Instant cutoff);
}
