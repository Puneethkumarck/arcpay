package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.infrastructure.temporal.SanctionsSnapshotWriter;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.SOME_OFAC_SDN_RECORD;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.SOME_UN_RECORD;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;
import static org.assertj.core.api.Assertions.assertThat;

class SanctionsSnapshotWriterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SanctionsSnapshotWriter snapshotWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM sanctioned_address");
        jdbcTemplate.update("DELETE FROM current_list_version");
        jdbcTemplate.update("DELETE FROM sanctions_list_version");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldPersistVersionRowWithActiveStatusAndAddresses() {
        // given
        var versionId = UUID.randomUUID();
        var records = Map.of(
                OFAC_SDN, List.of(SOME_OFAC_SDN_RECORD),
                UN, List.of(SOME_UN_RECORD));

        // when
        snapshotWriter.persistSnapshot(versionId, "checksum-abc", records);

        // then
        var status = jdbcTemplate.queryForObject(
                "SELECT status FROM sanctions_list_version WHERE version_id = ?", String.class, versionId);
        var addressCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sanctioned_address WHERE version_id = ?", Integer.class, versionId);
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(addressCount).isEqualTo(2);
    }

    @Test
    void shouldFlipPointerToLatestVersion() {
        // given
        var firstVersion = UUID.randomUUID();
        var secondVersion = UUID.randomUUID();

        // when
        snapshotWriter.flipCurrentVersion(firstVersion);
        snapshotWriter.flipCurrentVersion(secondVersion);

        // then
        var pointer = jdbcTemplate.queryForObject(
                "SELECT version_id FROM current_list_version WHERE id = 1", UUID.class);
        assertThat(pointer).isEqualTo(secondVersion);
    }

    @Test
    void shouldKeepSingletonPointerWhenFlippingSameVersionTwice() {
        // given
        var versionId = UUID.randomUUID();

        // when
        snapshotWriter.flipCurrentVersion(versionId);
        snapshotWriter.flipCurrentVersion(versionId);

        // then
        var rowCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM current_list_version", Integer.class);
        var pointer = jdbcTemplate.queryForObject(
                "SELECT version_id FROM current_list_version WHERE id = 1", UUID.class);
        assertThat(rowCount).isEqualTo(1);
        assertThat(pointer).isEqualTo(versionId);
    }
}
