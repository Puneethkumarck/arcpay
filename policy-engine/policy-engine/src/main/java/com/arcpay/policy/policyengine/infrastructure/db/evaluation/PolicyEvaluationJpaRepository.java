package com.arcpay.policy.policyengine.infrastructure.db.evaluation;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyEvaluationJpaRepository extends JpaRepository<PolicyEvaluationEntity, UUID> {

    void deleteByEvaluatedAtBefore(Instant cutoff);
}
