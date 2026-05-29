package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.event.PolicyViolationDetected;
import com.arcpay.policy.policyengine.domain.exception.PolicyHashMismatchException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.spending.SpendingLedgerService;
import com.arcpay.policy.policyengine.domain.spending.SpendingLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.FAIL_DAILY;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.FAIL_PER_TX;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.PASS_DAILY;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.PASS_PER_TX;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.REQUIRES_APPROVAL_THRESHOLD;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.SOME_AMOUNT;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.SOME_RECIPIENT;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.SOME_REQUESTED_AT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_SPENDING_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PolicyEvaluationServiceTest {

    private static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-0000000000a1");
    private static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-0000000000a2");
    private static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-0000000000a3");
    private static final String SOME_HASH = "0xhash";

    private static final PolicyRule.PerTransactionLimit SOME_PER_TX =
            new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"));
    private static final PolicyRule.DailyLimit SOME_DAILY = new PolicyRule.DailyLimit(new BigDecimal("1000.00"));
    private static final PolicyRule.Velocity SOME_VELOCITY = new PolicyRule.Velocity(50, 60);
    private static final PolicyRule.ApprovalThreshold SOME_APPROVAL =
            new PolicyRule.ApprovalThreshold(new BigDecimal("20.00"));

    private static final AgentInfo SOME_AGENT = new AgentInfo(SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", SOME_HASH);

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyEvaluationRepository policyEvaluationRepository;

    @Mock
    private SpendingLockService spendingLockService;

    @Mock
    private SpendingLedgerService spendingLedgerService;

    @Mock
    private RuleEvaluatorRegistry ruleEvaluatorRegistry;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private RuleEvaluator<PolicyRule.PerTransactionLimit> perTxEvaluator;

    @Mock
    private RuleEvaluator<PolicyRule.DailyLimit> dailyEvaluator;

    @Mock
    private RuleEvaluator<PolicyRule.Velocity> velocityEvaluator;

    @Mock
    private RuleEvaluator<PolicyRule.ApprovalThreshold> approvalEvaluator;

    private PolicyEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new PolicyEvaluationService(policyRepository, policyEvaluationRepository,
                spendingLockService, spendingLedgerService,
                ruleEvaluatorRegistry, eventPublisher);
    }

    private static Policy policyWith(PolicyRule... rules) {
        return Policy.builder()
                .policyId(SOME_POLICY_ID)
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .version(1)
                .rules(List.of(rules))
                .policyHash(SOME_HASH)
                .status(PolicyStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private void givenActivePolicy(Policy policy) {
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(policy));
    }

    private PolicyEvaluationResult invoke(boolean dryRun) {
        return service.evaluate(SOME_AGENT_ID, SOME_AGENT, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, dryRun);
    }

    private PolicyEvaluationResult invoke(AgentInfo agent, boolean dryRun) {
        return service.evaluate(SOME_AGENT_ID, agent, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, dryRun);
    }

    private static EvaluationContext context(boolean dryRun) {
        return EvaluationContext.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .policyId(SOME_POLICY_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .requestedAt(SOME_REQUESTED_AT)
                .dryRun(dryRun)
                .build();
    }

    private static EvaluationContext ctx(boolean dryRun) {
        return eqIgnoring(context(dryRun), "spendingSummary");
    }

    private PolicyEvaluationResult expectedResult(PolicyVerdict verdict, boolean dryRun,
            List<RuleEvaluationResult> ruleResults) {
        return PolicyEvaluationResult.builder()
                .evaluationId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(verdict)
                .ruleResults(ruleResults)
                .requestedAmount(SOME_AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(dryRun)
                .evaluatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .durationMs(0)
                .build();
    }

    @Nested
    class HappyPaths {

        @Test
        void shouldApproveWhenAllRulesPass() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX, SOME_DAILY));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(PASS_PER_TX);
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.DailyLimit.class)).willReturn(dailyEvaluator);
            given(dailyEvaluator.evaluate(eqIgnoring(SOME_DAILY), ctx(false)))
                    .willReturn(PASS_DAILY);
            given(spendingLedgerService.getSpendingSummary(SOME_AGENT_ID, 0)).willReturn(SOME_SPENDING_SUMMARY);

            // when
            var result = invoke(false);

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("evaluationId", "evaluatedAt", "durationMs")
                    .isEqualTo(expectedResult(PolicyVerdict.APPROVED, false, List.of(PASS_PER_TX, PASS_DAILY)));
        }

        @Test
        void shouldReturnRequiresApprovalWhenThresholdTriggered() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX, SOME_APPROVAL));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(PASS_PER_TX);
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.ApprovalThreshold.class)).willReturn(approvalEvaluator);
            given(approvalEvaluator.evaluate(eqIgnoring(SOME_APPROVAL), ctx(false)))
                    .willReturn(REQUIRES_APPROVAL_THRESHOLD);

            // when
            var result = invoke(false);

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("evaluationId", "evaluatedAt", "durationMs")
                    .isEqualTo(expectedResult(PolicyVerdict.REQUIRES_APPROVAL, false,
                            List.of(PASS_PER_TX, REQUIRES_APPROVAL_THRESHOLD)));
        }
    }

    @Nested
    class Rejections {

        @Test
        void shouldRejectWhenAnyRuleFails() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(FAIL_PER_TX);

            // when
            var result = invoke(false);

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("evaluationId", "evaluatedAt", "durationMs")
                    .isEqualTo(expectedResult(PolicyVerdict.REJECTED, false, List.of(FAIL_PER_TX)));
        }

        @Test
        void shouldFailFastInRealMode() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX, SOME_DAILY));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(FAIL_PER_TX);

            // when
            var result = invoke(false);

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("evaluationId", "evaluatedAt", "durationMs")
                    .isEqualTo(expectedResult(PolicyVerdict.REJECTED, false, List.of(FAIL_PER_TX)));
            then(spendingLedgerService).should(never()).getSpendingSummary(SOME_AGENT_ID, 0);
            then(dailyEvaluator).shouldHaveNoInteractions();
        }

        @Test
        void shouldEvaluateAllRulesInDryRunMode() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX, SOME_DAILY));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(true)))
                    .willReturn(FAIL_PER_TX);
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.DailyLimit.class)).willReturn(dailyEvaluator);
            given(dailyEvaluator.evaluate(eqIgnoring(SOME_DAILY), ctx(true)))
                    .willReturn(FAIL_DAILY);
            given(spendingLedgerService.getSpendingSummary(SOME_AGENT_ID, 0)).willReturn(SOME_SPENDING_SUMMARY);

            // when
            var result = invoke(true);

            // then
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("evaluationId", "evaluatedAt", "durationMs")
                    .isEqualTo(expectedResult(PolicyVerdict.REJECTED, true, List.of(FAIL_PER_TX, FAIL_DAILY)));
        }
    }

    @Nested
    class SpendingLazyLoad {

        @Test
        void shouldFetchSpendingSummaryOnlyWhenSpendingRulesExist() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(PASS_PER_TX);

            // when
            invoke(false);

            // then
            then(spendingLedgerService).shouldHaveNoInteractions();
        }

        @Test
        void shouldFetchSpendingSummaryWithVelocityWindowFromVelocityRule() {
            // given
            givenActivePolicy(policyWith(SOME_VELOCITY, SOME_DAILY));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.DailyLimit.class)).willReturn(dailyEvaluator);
            given(dailyEvaluator.evaluate(eqIgnoring(SOME_DAILY), ctx(false)))
                    .willReturn(PASS_DAILY);
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.Velocity.class)).willReturn(velocityEvaluator);
            given(velocityEvaluator.evaluate(eqIgnoring(SOME_VELOCITY), ctx(false)))
                    .willReturn(PASS_DAILY);
            given(spendingLedgerService.getSpendingSummary(SOME_AGENT_ID, 60)).willReturn(SOME_SPENDING_SUMMARY);

            // when
            invoke(false);

            // then
            then(spendingLedgerService).should().getSpendingSummary(SOME_AGENT_ID, 60);
        }

        @Test
        void shouldAcquirePessimisticLockBeforeEvaluation() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(PASS_PER_TX);

            // when
            invoke(false);

            // then
            then(spendingLockService).should().acquireLock(SOME_AGENT_ID);
        }
    }

    @Nested
    class Persistence {

        @Test
        void shouldPersistRejectedEvaluation() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(FAIL_PER_TX);

            // when
            invoke(false);

            // then
            then(policyEvaluationRepository).should().save(eqIgnoring(
                    expectedResult(PolicyVerdict.REJECTED, false, List.of(FAIL_PER_TX)),
                    "evaluationId", "evaluatedAt", "durationMs"));
        }

        @Test
        void shouldPersistDryRunEvaluation() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(true)))
                    .willReturn(PASS_PER_TX);

            // when
            invoke(true);

            // then
            then(policyEvaluationRepository).should().save(eqIgnoring(
                    expectedResult(PolicyVerdict.APPROVED, true, List.of(PASS_PER_TX)),
                    "evaluationId", "evaluatedAt", "durationMs"));
        }

        @Test
        void shouldNotPersistApprovedRealEvaluation() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(PASS_PER_TX);

            // when
            invoke(false);

            // then
            then(policyEvaluationRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    class ViolationEvents {

        @Test
        void shouldPublishViolationEventOnRejection() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(false)))
                    .willReturn(FAIL_PER_TX);

            // when
            invoke(false);

            // then
            then(eventPublisher).should().publish(eqIgnoring(
                    new PolicyViolationDetected(
                            UUID.fromString("00000000-0000-0000-0000-000000000000"),
                            SOME_AGENT_ID, SOME_POLICY_ID, FAIL_PER_TX.ruleType(), FAIL_PER_TX.message(),
                            SOME_AMOUNT, Instant.parse("2026-01-01T00:00:00Z")),
                    "evaluationId", "detectedAt"));
        }

        @Test
        void shouldNotPublishViolationEventOnDryRunRejection() {
            // given
            givenActivePolicy(policyWith(SOME_PER_TX));
            given(ruleEvaluatorRegistry.getEvaluator(PolicyRule.PerTransactionLimit.class)).willReturn(perTxEvaluator);
            given(perTxEvaluator.evaluate(eqIgnoring(SOME_PER_TX), ctx(true)))
                    .willReturn(FAIL_PER_TX);

            // when
            invoke(true);

            // then
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    class FailureModes {

        @Test
        void shouldRejectWhenNoPolicyConfigured() {
            // given
            given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());

            // when
            // then
            assertThatThrownBy(() -> invoke(false))
                    .isInstanceOf(PolicyNotFoundException.class)
                    .hasMessageContaining("no policy configured");
        }

        @Test
        void shouldDetectPolicyHashMismatch() {
            // given
            given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(policyWith(SOME_PER_TX)));
            var mismatchedAgent = new AgentInfo(SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", "0xdifferent");

            // when
            // then
            assertThatThrownBy(() -> invoke(mismatchedAgent, false))
                    .isInstanceOf(PolicyHashMismatchException.class);
            then(spendingLockService).shouldHaveNoInteractions();
        }
    }
}
