package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WorkflowImpl(taskQueues = "ComplianceTaskQueue")
class SanctionsIngestionWorkflowImpl implements SanctionsIngestionWorkflow {

    private static final Logger log = Workflow.getLogger(SanctionsIngestionWorkflowImpl.class);

    private final SanctionsIngestionActivities sourceActivities = Workflow.newActivityStub(
            SanctionsIngestionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(60))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    private final SanctionsIngestionActivities flipActivities = Workflow.newActivityStub(
            SanctionsIngestionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(1)
                            .build())
                    .build());

    @Override
    public void runIngestion(String triggerTimestamp) {
        log.info("Starting sanctions ingestion triggered at {}", triggerTimestamp);

        var downloads = new LinkedHashMap<SanctionsSource, Promise<byte[]>>();
        for (var source : SanctionsSource.values()) {
            downloads.put(source, Async.function(sourceActivities::downloadSource, source));
        }

        var recordsBySource = new LinkedHashMap<SanctionsSource, List<SanctionedAddressRecord>>();
        for (var entry : downloads.entrySet()) {
            var source = entry.getKey();
            try {
                var rawData = entry.getValue().get();
                var records = sourceActivities.parseAddresses(source, rawData);
                recordsBySource.put(source, records);
                log.info("Source {} ingested {} records", source, records.size());
            } catch (ActivityFailure failure) {
                log.warn("Source {} failed all retries; recording staleness and continuing", source, failure);
            }
        }

        if (recordsBySource.isEmpty()) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "All sanctions sources failed", "ALL_SOURCES_FAILED");
        }

        var snapshot = Map.copyOf(recordsBySource);
        var versionId = sourceActivities.validateSnapshot(snapshot);
        sourceActivities.persistSnapshot(versionId, snapshot);
        flipActivities.flipCurrentVersion(versionId);

        log.info("Sanctions ingestion completed for version {} with {} successful sources",
                versionId, recordsBySource.size());
    }
}
