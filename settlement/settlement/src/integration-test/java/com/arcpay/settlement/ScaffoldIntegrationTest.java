package com.arcpay.settlement;

import com.arcpay.settlement.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ScaffoldIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrations() {
        // given
        var tableNames = "'settlement_transaction','settlement_outbox_record',"
                + "'settlement_outbox_instance','settlement_outbox_partition'";

        // when
        var tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN (" + tableNames + ")",
                Integer.class);

        // then
        assertThat(tableCount).isEqualTo(4);
    }

    @Test
    void shouldCreateCircleTxIdIndex() {
        // when
        var indexCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                        + "AND tablename = 'settlement_transaction' "
                        + "AND indexname = 'idx_settlement_transaction_circle_tx_id'",
                Integer.class);

        // then
        assertThat(indexCount).isEqualTo(1);
    }
}
