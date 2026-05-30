package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.policy.PolicyHashUtil;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationServiceIntegrationTest extends FullContextIntegrationTest {

    private static final String RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SIXTY = new BigDecimal("60.00");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SpendingLedgerService spendingLedgerService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCountActiveHeldReservationsTowardLimit() {
        // given
        var agent = persistDailyLimitPolicy("100.00");

        // when
        var first = reservationService.reserve(UUID.randomUUID(), agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());
        var second = reservationService.reserve(UUID.randomUUID(), agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());

        // then
        assertThat(first.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        assertThat(second.verdict()).isEqualTo(PolicyVerdict.REJECTED);
        assertThat(heldCount(agent.agentId())).isOne();
    }

    @Test
    void shouldArbitrateConcurrentReservesAgainstLastHeadroom() throws Exception {
        // given
        var agent = persistDailyLimitPolicy("100.00");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        var verdicts = new CopyOnWriteArrayList<PolicyVerdict>();

        // when
        for (var i = 0; i < 2; i++) {
            executor.submit(() -> {
                awaitStart(start);
                verdicts.add(reservationService.reserve(
                        UUID.randomUUID(), agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now()).verdict());
            });
        }
        start.countDown();
        executor.shutdown();
        var finished = executor.awaitTermination(20, TimeUnit.SECONDS);

        // then
        assertThat(finished).isTrue();
        assertThat(verdicts).containsExactlyInAnyOrder(PolicyVerdict.APPROVED, PolicyVerdict.REJECTED);
        assertThat(heldCount(agent.agentId())).isOne();
    }

    @Test
    void shouldBeIdempotentOnDuplicateReserve() {
        // given
        var agent = persistDailyLimitPolicy("1000.00");
        var paymentId = UUID.randomUUID();

        // when
        var first = reservationService.reserve(paymentId, agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());
        var second = reservationService.reserve(paymentId, agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());

        // then
        assertThat(first.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        assertThat(second.verdict()).isEqualTo(PolicyVerdict.APPROVED);
        assertThat(heldCount(agent.agentId())).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM spending_reservation WHERE agent_id = ? AND status = 'HELD'",
                BigDecimal.class, agent.agentId())).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldFoldSpendingIntoLedgerOnCommit() {
        // given
        var agent = persistDailyLimitPolicy("1000.00");
        var paymentId = UUID.randomUUID();
        reservationService.reserve(paymentId, agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());

        // when
        reservationService.commit(paymentId);

        // then
        assertThat(heldCount(agent.agentId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spending_ledger WHERE payment_id = ?", Integer.class, paymentId)).isOne();
        var summary = spendingLedgerService.getSpendingSummary(agent.agentId(), 60);
        assertThat(summary.dailyTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldReturnHeadroomAfterRelease() {
        // given
        var agent = persistDailyLimitPolicy("100.00");
        var firstPayment = UUID.randomUUID();
        reservationService.reserve(firstPayment, agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());

        // when
        reservationService.release(firstPayment);
        var afterRelease = reservationService.reserve(
                UUID.randomUUID(), agent.agentId(), agent, RECIPIENT, SIXTY, Instant.now());

        // then
        assertThat(afterRelease.verdict()).isEqualTo(PolicyVerdict.APPROVED);
    }

    private AgentInfo persistDailyLimitPolicy(String limit) {
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        List<PolicyRule> rules = List.of(new PolicyRule.DailyLimit(new BigDecimal(limit)));
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
        return new AgentInfo(agentId, ownerId, "ACTIVE", hash);
    }

    private int heldCount(UUID agentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spending_reservation WHERE agent_id = ? AND status = 'HELD'",
                Integer.class, agentId);
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
