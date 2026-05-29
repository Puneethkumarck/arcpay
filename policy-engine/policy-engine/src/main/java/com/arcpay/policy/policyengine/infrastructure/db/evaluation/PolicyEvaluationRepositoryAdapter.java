package com.arcpay.policy.policyengine.infrastructure.db.evaluation;

import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
class PolicyEvaluationRepositoryAdapter implements PolicyEvaluationRepository {

    private final PolicyEvaluationJpaRepository jpaRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PolicyEvaluationResult save(PolicyEvaluationResult result) {
        var entity = mapToEntity(result);
        var saved = jpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional
    public void deleteOlderThan(Instant cutoff) {
        jpaRepository.deleteByEvaluatedAtBefore(cutoff);
    }

    private PolicyEvaluationEntity mapToEntity(PolicyEvaluationResult result) {
        return PolicyEvaluationEntity.builder()
                .evaluationId(result.evaluationId())
                .agentId(result.agentId())
                .policyId(result.policyId())
                .verdict(result.verdict())
                .ruleResults(ruleResultsToJson(result.ruleResults()))
                .requestedAmount(result.requestedAmount())
                .recipientAddress(result.recipientAddress())
                .durationMs(Math.toIntExact(result.durationMs()))
                .dryRun(result.dryRun())
                .evaluatedAt(result.evaluatedAt())
                .build();
    }

    private PolicyEvaluationResult mapToDomain(PolicyEvaluationEntity entity) {
        return PolicyEvaluationResult.builder()
                .evaluationId(entity.getEvaluationId())
                .agentId(entity.getAgentId())
                .policyId(entity.getPolicyId())
                .verdict(entity.getVerdict())
                .ruleResults(jsonToRuleResults(entity.getRuleResults()))
                .requestedAmount(entity.getRequestedAmount())
                .recipientAddress(entity.getRecipientAddress())
                .dryRun(entity.isDryRun())
                .evaluatedAt(entity.getEvaluatedAt())
                .durationMs(entity.getDurationMs())
                .build();
    }

    private static String ruleResultsToJson(List<RuleEvaluationResult> results) {
        try {
            return OBJECT_MAPPER.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rule results to JSON", e);
        }
    }

    private static List<RuleEvaluationResult> jsonToRuleResults(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize rule results from JSON", e);
        }
    }
}
