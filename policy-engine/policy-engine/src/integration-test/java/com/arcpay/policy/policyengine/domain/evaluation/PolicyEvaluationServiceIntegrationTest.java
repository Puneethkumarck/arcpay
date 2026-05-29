package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.policy.PolicyHashUtil;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.spending.SpendingLedgerService;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Transactional
class PolicyEvaluationServiceIntegrationTest extends FullContextIntegrationTest {

    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    @Autowired
    private PolicyEvaluationService policyEvaluationService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private SpendingLedgerService spendingLedgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AgentServiceClient agentServiceClient;

    private int evaluationCount(String whereClause, Object... args) {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM policy_evaluations WHERE " + whereClause, Integer.class, args);
    }

    private UUID persistPolicy(UUID ownerId, List<PolicyRule> rules) {
        var agentId = UUID.randomUUID();
        var hash = PolicyHashUtil.computePolicyHash(rules);
        policyRepository.save(Policy.builder()
                .policyId(UUID.randomUUID())
                .agentId(agentId)
                .ownerId(ownerId)
                .version(1)
                .rules(rules)
                .policyHash(hash)
                .status(PolicyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        given(agentServiceClient.getAgent(agentId))
                .willReturn(Optional.of(new AgentInfo(agentId, ownerId, "ACTIVE", hash)));
        return agentId;
    }

    @Test
    void shouldApproveWhenAllRulesPassAgainstRealDatabase() {
        // given
        var ownerId = UUID.randomUUID();
        var agentId = persistPolicy(ownerId, List.of(
                new PolicyRule.PerTransactionLimit(new BigDecimal("100.00")),
                new PolicyRule.DailyLimit(new BigDecimal("1000.00"))));

        // when
        var result = policyEvaluationService.evaluate(
                agentId, SOME_RECIPIENT, new BigDecimal("30.00"), Instant.now(), false);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        assertThat(result.ruleResults()).hasSize(2);
        // approved real evaluations are not persisted
        assertThat(evaluationCount("agent_id = ?", agentId)).isZero();
    }

    @Test
    void shouldRejectAndPersistWhenSpendingLimitExceeded() {
        // given — prior spend of 80 plus requested 30 exceeds daily limit of 100
        var ownerId = UUID.randomUUID();
        var agentId = persistPolicy(ownerId, List.of(new PolicyRule.DailyLimit(new BigDecimal("100.00"))));
        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("80.000000"), SOME_RECIPIENT,
                Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS));

        // when
        var result = policyEvaluationService.evaluate(
                agentId, SOME_RECIPIENT, new BigDecimal("30.00"), Instant.now(), false);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.REJECTED);
        assertThat(evaluationCount("agent_id = ? AND verdict = 'REJECTED'", agentId)).isOne();
    }

    @Test
    void shouldReturnRequiresApprovalAndPersistWhenThresholdExceeded() {
        // given
        var ownerId = UUID.randomUUID();
        var agentId = persistPolicy(ownerId, List.of(new PolicyRule.ApprovalThreshold(new BigDecimal("20.00"))));

        // when
        var result = policyEvaluationService.evaluate(
                agentId, SOME_RECIPIENT, new BigDecimal("30.00"), Instant.now(), false);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.REQUIRES_APPROVAL);
        assertThat(evaluationCount("agent_id = ? AND verdict = 'REQUIRES_APPROVAL'", agentId)).isOne();
    }

    @Test
    void shouldPersistDryRunEvaluationEvenWhenApproved() {
        // given
        var ownerId = UUID.randomUUID();
        var agentId = persistPolicy(ownerId, List.of(new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"))));

        // when
        var result = policyEvaluationService.evaluate(
                agentId, SOME_RECIPIENT, new BigDecimal("30.00"), Instant.now(), true);

        // then
        assertThat(result.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        assertThat(result.dryRun()).isTrue();
        assertThat(evaluationCount("agent_id = ? AND dry_run = true", agentId)).isOne();
    }
}
