package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.event.PolicyViolationDetected;
import com.arcpay.policy.policyengine.domain.exception.PolicyHashMismatchException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.spending.SpendingLedgerService;
import com.arcpay.policy.policyengine.domain.spending.SpendingLockService;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private static final Set<Class<? extends PolicyRule>> IN_MEMORY_RULE_TYPES = Set.of(
            PolicyRule.PerTransactionLimit.class,
            PolicyRule.RecipientAllowlist.class,
            PolicyRule.RecipientBlocklist.class,
            PolicyRule.TimeWindow.class,
            PolicyRule.ApprovalThreshold.class);

    private final PolicyRepository policyRepository;
    private final PolicyEvaluationRepository policyEvaluationRepository;
    private final SpendingLockService spendingLockService;
    private final SpendingLedgerService spendingLedgerService;
    private final RuleEvaluatorRegistry ruleEvaluatorRegistry;
    private final EventPublisher eventPublisher;

    @Transactional
    public PolicyEvaluationResult evaluate(UUID agentId, AgentInfo agent, String recipientAddress,
            BigDecimal amount, Instant requestedAt, boolean dryRun) {
        var startNanos = System.nanoTime();

        var policy = policyRepository.findActiveByAgentId(agentId)
                .orElseThrow(() -> new PolicyNotFoundException(agentId, "no policy configured"));

        if (!policy.policyHash().equals(agent.policyHash())) {
            throw new PolicyHashMismatchException(agentId, policy.policyHash(), agent.policyHash());
        }

        spendingLockService.acquireLock(agentId);

        var context = buildContext(agentId, agent, policy, recipientAddress, amount, requestedAt, dryRun);
        var results = new ArrayList<RuleEvaluationResult>();

        // Phase 1: in-memory rules
        var inMemoryRules = classifyRules(policy.rules(), true);
        for (var rule : inMemoryRules) {
            var result = evaluateRule(rule, context);
            results.add(result);
            if (!dryRun && result.verdict() == RuleVerdict.FAIL) {
                return complete(context, policy, results, startNanos);
            }
        }

        // Phase 2: lazily fetch spending summary only when spending-dependent rules exist
        var spendingRules = classifyRules(policy.rules(), false);
        if (!spendingRules.isEmpty()) {
            var velocityMinutes = extractVelocityMinutes(spendingRules);
            var summary = spendingLedgerService.getSpendingSummary(agentId, velocityMinutes);
            context = context.toBuilder().spendingSummary(summary).build();
        }

        // Phase 3: spending-dependent rules
        for (var rule : spendingRules) {
            var result = evaluateRule(rule, context);
            results.add(result);
            if (!dryRun && result.verdict() == RuleVerdict.FAIL) {
                return complete(context, policy, results, startNanos);
            }
        }

        return complete(context, policy, results, startNanos);
    }

    private PolicyEvaluationResult complete(EvaluationContext context, Policy policy,
            List<RuleEvaluationResult> results, long startNanos) {
        var evalResult = buildResult(context, policy, results, startNanos);

        // Persist rejections, requires-approval, and all dry-runs; approved real
        // evaluations are transient (captured downstream by the Audit Ledger).
        if (evalResult.verdict() != PolicyVerdict.APPROVED || evalResult.dryRun()) {
            policyEvaluationRepository.save(evalResult);
        }

        if (evalResult.verdict() == PolicyVerdict.REJECTED && !evalResult.dryRun()) {
            publishViolationEvent(evalResult, results);
        }

        log.info("Evaluation complete agentId={} policyId={} verdict={} dryRun={} durationMs={}",
                evalResult.agentId(), evalResult.policyId(), evalResult.verdict(),
                evalResult.dryRun(), evalResult.durationMs());
        return evalResult;
    }

    private EvaluationContext buildContext(UUID agentId, AgentInfo agent, Policy policy,
            String recipientAddress, BigDecimal amount, Instant requestedAt, boolean dryRun) {
        return EvaluationContext.builder()
                .agentId(agentId)
                .ownerId(agent.ownerId())
                .policyId(policy.policyId())
                .recipientAddress(recipientAddress)
                .amount(amount)
                .requestedAt(requestedAt)
                .dryRun(dryRun)
                .build();
    }

    private List<PolicyRule> classifyRules(List<PolicyRule> rules, boolean inMemory) {
        return rules.stream()
                .filter(rule -> IN_MEMORY_RULE_TYPES.contains(rule.getClass()) == inMemory)
                .toList();
    }

    private int extractVelocityMinutes(List<PolicyRule> spendingRules) {
        return spendingRules.stream()
                .filter(PolicyRule.Velocity.class::isInstance)
                .map(PolicyRule.Velocity.class::cast)
                .mapToInt(PolicyRule.Velocity::periodMinutes)
                .findFirst()
                .orElse(0);
    }

    private <T extends PolicyRule> RuleEvaluationResult evaluateRule(T rule, EvaluationContext context) {
        @SuppressWarnings("unchecked")
        var ruleType = (Class<T>) rule.getClass();
        return ruleEvaluatorRegistry.getEvaluator(ruleType).evaluate(rule, context);
    }

    private PolicyEvaluationResult buildResult(EvaluationContext context, Policy policy,
            List<RuleEvaluationResult> results, long startNanos) {
        var verdict = determineVerdict(results);
        var now = Instant.now();
        return PolicyEvaluationResult.builder()
                .evaluationId(UuidCreator.getTimeOrderedEpoch())
                .agentId(context.agentId())
                .policyId(policy.policyId())
                .verdict(verdict)
                .ruleResults(results)
                .requestedAmount(context.amount())
                .recipientAddress(context.recipientAddress())
                .dryRun(context.dryRun())
                .evaluatedAt(now)
                .durationMs((System.nanoTime() - startNanos) / 1_000_000)
                .build();
    }

    private PolicyVerdict determineVerdict(List<RuleEvaluationResult> results) {
        var requiresApproval = false;
        for (var result : results) {
            if (result.verdict() == RuleVerdict.FAIL) {
                return PolicyVerdict.REJECTED;
            }
            if (result.verdict() == RuleVerdict.REQUIRES_APPROVAL) {
                requiresApproval = true;
            }
        }
        return requiresApproval ? PolicyVerdict.REQUIRES_APPROVAL : PolicyVerdict.APPROVED;
    }

    private void publishViolationEvent(PolicyEvaluationResult evalResult, List<RuleEvaluationResult> results) {
        var violated = results.stream()
                .filter(result -> result.verdict() == RuleVerdict.FAIL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Rejected evaluation has no failing rule for agent " + evalResult.agentId()));

        eventPublisher.publish(new PolicyViolationDetected(
                evalResult.evaluationId(),
                evalResult.agentId(),
                evalResult.policyId(),
                violated.ruleType(),
                violated.message(),
                evalResult.requestedAmount(),
                Instant.now()));
    }
}
