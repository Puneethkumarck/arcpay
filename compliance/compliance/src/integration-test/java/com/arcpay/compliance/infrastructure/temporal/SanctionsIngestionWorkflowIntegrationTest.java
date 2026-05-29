package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.SOME_TRIGGER_TIMESTAMP;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.addressFor;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.feedFor;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class SanctionsIngestionWorkflowIntegrationTest extends FullContextIntegrationTest {

    @MockitoBean
    private SanctionsFeedDownloader downloader;

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private SanctionsRefreshTracker refreshTracker;

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
    void shouldIngestAllSourcesPersistSnapshotAndFlipPointer() {
        // given
        for (var source : SanctionsSource.values()) {
            given(downloader.download(source)).willReturn(feedFor(source));
        }

        // when
        runIngestion(SOME_TRIGGER_TIMESTAMP);

        // then
        var versionId = currentPointerVersionId();
        assertThat(versionId).isNotNull();
        var addresses = addressesForVersion(versionId);
        var expected = java.util.Arrays.stream(SanctionsSource.values())
                .map(com.arcpay.compliance.fixtures.SanctionsIngestionFixtures::addressFor)
                .toList();
        assertThat(addresses).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void shouldCompleteWithRemainingSourcesWhenOneSourceFailsAllRetries() {
        // given
        for (var source : SanctionsSource.values()) {
            given(downloader.download(source)).willReturn(feedFor(source));
        }
        given(downloader.download(OFAC_SDN)).willThrow(new RuntimeException("network timeout"));

        // when
        runIngestion(SOME_TRIGGER_TIMESTAMP);

        // then
        var versionId = currentPointerVersionId();
        var addresses = addressesForVersion(versionId);
        assertThat(addresses).doesNotContain(addressFor(OFAC_SDN));
        assertThat(addresses).contains(addressFor(UN));
        assertThat(refreshTracker.lastSuccessfulRefresh(OFAC_SDN)).isEmpty();
        assertThat(refreshTracker.lastSuccessfulRefresh(UN)).isPresent();
    }

    @Test
    void shouldFlipPointerToPersistedVersionInSingleTransaction() {
        // given
        for (var source : SanctionsSource.values()) {
            given(downloader.download(source)).willReturn(feedFor(source));
        }

        // when
        runIngestion(SOME_TRIGGER_TIMESTAMP);

        // then
        var pointerVersionId = currentPointerVersionId();
        var persistedVersionIds = jdbcTemplate.queryForList(
                "SELECT version_id FROM sanctions_list_version", UUID.class);
        assertThat(persistedVersionIds).containsExactly(pointerVersionId);
    }

    private void runIngestion(String triggerTimestamp) {
        var workflow = workflowClient.newWorkflowStub(
                SanctionsIngestionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(SanctionsIngestionWorkflow.WORKFLOW_ID + "_" + triggerTimestamp)
                        .setTaskQueue(SanctionsIngestionWorkflow.TASK_QUEUE)
                        .setWorkflowExecutionTimeout(Duration.ofMinutes(2))
                        .build());
        workflow.runIngestion(triggerTimestamp);
    }

    private UUID currentPointerVersionId() {
        return jdbcTemplate.queryForObject(
                "SELECT version_id FROM current_list_version WHERE id = 1", UUID.class);
    }

    private List<String> addressesForVersion(UUID versionId) {
        return jdbcTemplate.queryForList(
                "SELECT address FROM sanctioned_address WHERE version_id = ?", String.class, versionId);
    }
}
