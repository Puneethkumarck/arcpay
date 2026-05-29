package com.arcpay.compliance;

import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.fixtures.SanctionsIngestionFixtures;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import com.arcpay.compliance.infrastructure.temporal.SanctionsFeedDownloader;
import com.arcpay.compliance.infrastructure.temporal.SanctionsIngestionWorkflow;
import com.arcpay.compliance.infrastructure.temporal.SanctionsRefreshTracker;
import com.arcpay.compliance.test.BusinessTest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.SOME_TRIGGER_TIMESTAMP;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.addressFor;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.feedFor;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SanctionsIngestionE2ETest extends BusinessTest {

    @MockitoBean
    private SanctionsFeedDownloader downloader;

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private SanctionsRefreshTracker refreshTracker;

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @BeforeEach
    void resetState() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldIngestAllSourcesFlipPointerAndSwapInMemorySet() {
        // given
        for (var source : SanctionsSource.values()) {
            given(downloader.download(source)).willReturn(feedFor(source));
        }

        // when
        runIngestion(SOME_TRIGGER_TIMESTAMP);

        // then
        var versionId = currentPointerVersionId();
        assertThat(versionId).isNotNull();
        var expected = Arrays.stream(SanctionsSource.values())
                .map(SanctionsIngestionFixtures::addressFor)
                .toList();
        assertThat(addressesForVersion(versionId)).containsExactlyInAnyOrderElementsOf(expected);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var snapshot = sanctionsSetProvider.getCurrentSanctionsSet();
            assertThat(snapshot.versionId()).isEqualTo(versionId);
            assertThat(expected).allMatch(snapshot::contains);
        });
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
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var snapshot = sanctionsSetProvider.getCurrentSanctionsSet();
            assertThat(snapshot.versionId()).isEqualTo(versionId);
            assertThat(snapshot.contains(addressFor(UN))).isTrue();
            assertThat(snapshot.contains(addressFor(OFAC_SDN))).isFalse();
        });
    }

    @Test
    void shouldReplacePointerWithSinglePersistedVersionOnIngestion() {
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
