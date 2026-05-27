package com.arcpay.policy.policyengine.infrastructure.db;

import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrationsSuccessfully() {
        // given — Flyway runs automatically on context startup

        // when
        var tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                String.class
        );

        // then
        assertThat(tables).contains(
                "policies",
                "policy_evaluations",
                "policyengine_outbox_instance",
                "policyengine_outbox_partition",
                "policyengine_outbox_record",
                "spending_ledger",
                "spending_locks"
        );
    }

    @Test
    void shouldCreatePoliciesTableWithCorrectColumns() {
        // when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'policies' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "policy_id", "agent_id", "owner_id", "version", "rules",
                "policy_hash", "status", "created_at", "updated_at"
        );
    }

    @Test
    void shouldCreateSpendingLedgerWithCorrectColumns() {
        // when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'spending_ledger' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "entry_id", "agent_id", "payment_id", "amount", "recipient",
                "executed_at", "created_at"
        );
    }

    @Test
    void shouldCreatePolicyEvaluationsWithCorrectColumns() {
        // when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'policy_evaluations' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "evaluation_id", "agent_id", "policy_id", "verdict", "rule_results",
                "requested_amount", "recipient_address", "duration_ms", "dry_run", "evaluated_at"
        );
    }

    @Test
    void shouldCreateOutboxTablesWithCorrectPrefix() {
        // when
        var tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_name LIKE 'policyengine_outbox%' ORDER BY table_name",
                String.class
        );

        // then
        assertThat(tables).containsExactly(
                "policyengine_outbox_instance",
                "policyengine_outbox_partition",
                "policyengine_outbox_record"
        );
    }
}
