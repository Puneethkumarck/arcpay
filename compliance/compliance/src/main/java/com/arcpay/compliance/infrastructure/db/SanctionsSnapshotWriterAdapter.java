package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import com.arcpay.compliance.infrastructure.temporal.SanctionsSnapshotWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Slf4j
@Component
@RequiredArgsConstructor
class SanctionsSnapshotWriterAdapter implements SanctionsSnapshotWriter {

    private static final Short CURRENT_POINTER_ID = 1;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String AGGREGATE_SOURCE = "ALL";

    private final SanctionsListVersionRepository sanctionsListVersionRepository;
    private final SanctionedAddressRepository sanctionedAddressRepository;
    private final CurrentListVersionRepository currentListVersionRepository;

    @Override
    @Transactional
    public void persistSnapshot(UUID versionId, String checksum,
                                Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource) {
        var now = Instant.now();
        var addressEntities = recordsBySource.values().stream()
                .flatMap(List::stream)
                .map(record -> toAddressEntity(versionId, record))
                .toList();

        var versionEntity = SanctionsListVersionEntity.builder()
                .versionId(versionId)
                .source(AGGREGATE_SOURCE)
                .downloadedAt(now)
                .recordCount(addressEntities.size())
                .checksum(checksum)
                .status(STATUS_ACTIVE)
                .build();

        sanctionsListVersionRepository.save(versionEntity);
        sanctionedAddressRepository.saveAll(addressEntities);
        log.info("Persisted sanctions snapshot version {} with {} addresses across {} sources",
                versionId, addressEntities.size(), recordsBySource.size());
    }

    @Override
    @Transactional
    public void flipCurrentVersion(UUID versionId) {
        var now = Instant.now();
        var pointer = currentListVersionRepository.findById(CURRENT_POINTER_ID)
                .map(existing -> {
                    existing.setVersionId(versionId);
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> CurrentListVersionEntity.builder()
                        .id(CURRENT_POINTER_ID)
                        .versionId(versionId)
                        .updatedAt(now)
                        .build());
        currentListVersionRepository.save(pointer);
        log.info("Flipped current sanctions list version pointer to {}", versionId);
    }

    private SanctionedAddressEntity toAddressEntity(UUID versionId, SanctionedAddressRecord record) {
        return SanctionedAddressEntity.builder()
                .id(randomUUID())
                .versionId(versionId)
                .address(record.address())
                .source(record.source().name())
                .sourceRef(record.sourceRef())
                .build();
    }
}
