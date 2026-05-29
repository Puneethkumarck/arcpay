package com.arcpay.compliance.infrastructure.sanctions;

import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.uniqueEvmAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
class SanctionsCacheIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSwapCacheToNewVersionWithinPollingWindow() {
        // given
        var versionId = UuidCreator.getTimeOrderedEpoch();
        var normalized = uniqueEvmAddress();
        seedSanctionsVersion(versionId, normalized);

        // when / then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var snapshot = sanctionsSetProvider.getCurrentSanctionsSet();
            assertThat(snapshot.versionId()).isEqualTo(versionId);
            assertThat(snapshot.contains(normalized)).isTrue();
        });
    }

    @Test
    void shouldExposeLockFreeReadsToConcurrentReaders() {
        // given
        var versionId = UuidCreator.getTimeOrderedEpoch();
        var normalized = "0xcccccccccccccccccccccccccccccccccccccccc";
        seedSanctionsVersion(versionId, normalized);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> versionId.equals(sanctionsSetProvider.getCurrentSanctionsSet().versionId()));

        // when
        var allReadsConsistent = java.util.stream.IntStream.range(0, 256).parallel()
                .allMatch(i -> {
                    var snapshot = sanctionsSetProvider.getCurrentSanctionsSet();
                    return snapshot.versionId() != null && snapshot.contains(normalized);
                });

        // then
        assertThat(allReadsConsistent).isTrue();
    }

    private void seedSanctionsVersion(UUID versionId, String address) {
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, address, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
    }
}
