package com.arcpay.policy.policyengine.domain.port;

import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;

import java.time.Instant;

public interface PolicyEvaluationRepository {

    PolicyEvaluationResult save(PolicyEvaluationResult result);

    void deleteOlderThan(Instant cutoff);
}
