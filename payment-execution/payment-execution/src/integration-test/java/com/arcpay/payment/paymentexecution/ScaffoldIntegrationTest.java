package com.arcpay.payment.paymentexecution;

import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScaffoldIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrations() {
        // given
        var tableNames = "'payment','paymentexecution_outbox_record',"
                + "'paymentexecution_outbox_instance','paymentexecution_outbox_partition'";

        // when
        var tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN (" + tableNames + ")",
                Integer.class);

        // then
        assertThat(tableCount).isEqualTo(4);
    }

    @Test
    void shouldRejectDuplicateIdempotencyKeyForSameAgent() {
        // given
        var agentId = UUID.randomUUID().toString();
        var idempotencyKey = "idem-key-1";
        insertPayment(agentId, idempotencyKey);

        // when / then
        assertThatThrownBy(() -> insertPayment(agentId, idempotencyKey))
                .isInstanceOf(DataAccessException.class);
    }

    private void insertPayment(String agentId, String idempotencyKey) {
        jdbcTemplate.update(
                "INSERT INTO payment (payment_id, agent_id, owner_id, idempotency_key, "
                        + "request_fingerprint, recipient_address, amount, currency, status, "
                        + "metadata, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)",
                UUID.randomUUID().toString(),
                agentId,
                UUID.randomUUID().toString(),
                idempotencyKey,
                "0xfingerprint",
                "0xrecipient",
                new BigDecimal("25.000000"),
                "USDC",
                "PENDING",
                "{\"category\":\"compute\"}",
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }
}
