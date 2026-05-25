package com.arcpay.identity.agentidentity.infrastructure.db;

import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
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
                "agents",
                "agentidentity_outbox_instance",
                "agentidentity_outbox_partition",
                "agentidentity_outbox_record",
                "gas_usage",
                "idempotency_keys",
                "owners"
        );
    }

    @Test
    void shouldCreateOwnersTableWithCorrectColumns() {
        // given / when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'owners' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "owner_id", "email", "wallet_address", "api_key_hash", "status", "created_at", "updated_at"
        );
    }

    @Test
    void shouldCreateOwnersUniqueIndexes() {
        // given / when
        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'owners' ORDER BY indexname",
                String.class
        );

        // then
        assertThat(indexes).contains(
                "idx_owners_email",
                "idx_owners_wallet",
                "idx_owners_api_key_hash",
                "owners_pkey"
        );
    }

    @Test
    void shouldCreateAgentsTableWithCorrectColumns() {
        // given / when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'agents' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "agent_id", "owner_id", "name", "purpose", "status",
                "wallet_id", "wallet_address", "on_chain_tx_hash",
                "policy_hash", "metadata_hash", "failure_reason",
                "created_at", "updated_at"
        );
    }

    @Test
    void shouldCreateAgentsUniqueIndexOnOwnerAndName() {
        // given / when
        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'agents' ORDER BY indexname",
                String.class
        );

        // then
        assertThat(indexes).contains(
                "idx_agents_owner_name",
                "idx_agents_owner_id",
                "idx_agents_status",
                "agents_pkey"
        );
    }

    @Test
    void shouldCreateAgentsForeignKeyToOwners() {
        // given / when
        var fks = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_name = 'agents' AND constraint_type = 'FOREIGN KEY'",
                String.class
        );

        // then
        assertThat(fks).contains("agents_owner_fk");
    }

    @Test
    void shouldCreateIdempotencyKeysWithCompositePrimaryKey() {
        // given / when
        var pkColumns = jdbcTemplate.queryForList(
                "SELECT a.attname FROM pg_index i " +
                        "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                        "JOIN pg_class c ON c.oid = i.indrelid " +
                        "WHERE c.relname = 'idempotency_keys' AND i.indisprimary " +
                        "ORDER BY a.attnum",
                String.class
        );

        // then
        assertThat(pkColumns).containsExactly("idempotency_key", "owner_id");
    }

    @Test
    void shouldCreateIdempotencyKeysExpiresIndex() {
        // given / when
        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'idempotency_keys' AND indexname = 'idx_idempotency_expires'",
                String.class
        );

        // then
        assertThat(indexes).containsExactly("idx_idempotency_expires");
    }

    @Test
    void shouldCreateOutboxTablesWithCorrectPrefix() {
        // given / when
        var outboxTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name LIKE 'agentidentity_outbox_%' ORDER BY table_name",
                String.class
        );

        // then
        assertThat(outboxTables).containsExactly(
                "agentidentity_outbox_instance",
                "agentidentity_outbox_partition",
                "agentidentity_outbox_record"
        );
    }

    @Test
    void shouldCreateGasUsageTableWithForeignKey() {
        // given / when
        var fks = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_name = 'gas_usage' AND constraint_type = 'FOREIGN KEY'",
                String.class
        );

        // then
        assertThat(fks).contains("gas_usage_owner_fk");
    }

    @Test
    void shouldCreateGasUsageWithCorrectColumns() {
        // given / when
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'gas_usage' ORDER BY ordinal_position",
                String.class
        );

        // then
        assertThat(columns).containsExactly(
                "id", "owner_id", "agent_id", "operation", "tx_hash",
                "gas_used", "gas_cost_usdc", "created_at"
        );
    }

    @Test
    void shouldCreateGasUsageOwnerIndex() {
        // given / when
        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'gas_usage' AND indexname = 'idx_gas_usage_owner'",
                String.class
        );

        // then
        assertThat(indexes).containsExactly("idx_gas_usage_owner");
    }
}
