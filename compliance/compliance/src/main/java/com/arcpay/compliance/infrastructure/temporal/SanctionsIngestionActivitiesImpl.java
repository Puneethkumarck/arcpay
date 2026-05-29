package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import com.arcpay.compliance.infrastructure.sanctions.parser.ParserRegistry;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "ComplianceTaskQueue")
class SanctionsIngestionActivitiesImpl implements SanctionsIngestionActivities {

    private static final int MINIMUM_RECORD_COUNT = 1;
    private static final int MAXIMUM_RECORD_COUNT = 10_000_000;

    private final SanctionsFeedDownloader downloader;
    private final ParserRegistry parserRegistry;
    private final SanctionsSnapshotWriter snapshotWriter;
    private final SanctionsRefreshTracker refreshTracker;

    @Override
    public byte[] downloadSource(SanctionsSource source) {
        log.info("Downloading sanctions source {}", source);
        return downloader.download(source);
    }

    @Override
    public List<SanctionedAddressRecord> parseAddresses(SanctionsSource source, byte[] rawData) {
        log.info("Parsing sanctions source {} ({} bytes)", source, rawData.length);
        var content = new String(rawData, StandardCharsets.UTF_8);
        return parserRegistry.parserFor(source).parse(content);
    }

    @Override
    public UUID validateSnapshot(Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource) {
        var totalRecords = recordsBySource.values().stream().mapToInt(List::size).sum();
        if (totalRecords < MINIMUM_RECORD_COUNT) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Sanctions snapshot is empty", "EMPTY_SNAPSHOT");
        }
        if (totalRecords > MAXIMUM_RECORD_COUNT) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Sanctions snapshot exceeds sanity bound: " + totalRecords, "OVERSIZED_SNAPSHOT");
        }
        log.info("Validated sanctions snapshot with {} records across {} sources",
                totalRecords, recordsBySource.size());
        return randomUUID();
    }

    @Override
    public void persistSnapshot(UUID versionId,
                                Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource) {
        var checksum = checksum(recordsBySource);
        snapshotWriter.persistSnapshot(versionId, checksum, recordsBySource);
        var now = Instant.now();
        recordsBySource.keySet().forEach(source -> refreshTracker.recordSuccess(source, now));
    }

    @Override
    public void flipCurrentVersion(UUID versionId) {
        snapshotWriter.flipCurrentVersion(versionId);
    }

    private String checksum(Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource) {
        var canonical = new StringBuilder();
        new TreeMap<>(recordsBySource).forEach((source, records) -> {
            canonical.append(source.name()).append(':');
            records.stream()
                    .map(SanctionedAddressRecord::address)
                    .sorted()
                    .forEach(address -> canonical.append(address).append(','));
            canonical.append(';');
        });
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
