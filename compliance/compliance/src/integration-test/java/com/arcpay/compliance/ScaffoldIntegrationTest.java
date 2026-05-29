package com.arcpay.compliance;

import com.arcpay.compliance.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScaffoldIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrations() {
        // given
        var tableNames = "'sanctions_list_version','sanctioned_address','current_list_version',"
                + "'watchlist_address','screening_result','screening_check','hold_review','compliance_outbox_record'";

        // when
        var tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN (" + tableNames + ")",
                Integer.class);

        // then
        assertThat(tableCount).isEqualTo(8);
    }

    @Test
    void shouldRejectSecondRowInCurrentListVersion() {
        // given
        var nonOneId = 2;

        // when / then
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id) VALUES (?, ?)", nonOneId, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
    }
}
