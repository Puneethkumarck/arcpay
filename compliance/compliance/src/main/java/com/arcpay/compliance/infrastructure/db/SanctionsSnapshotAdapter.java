package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.SanctionsSet;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class SanctionsSnapshotAdapter implements SanctionsSetProvider {

    private static final Short CURRENT_POINTER_ID = 1;

    private final CurrentListVersionRepository currentListVersionRepository;
    private final SanctionedAddressRepository sanctionedAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public SanctionsSet getCurrentSanctionsSet() {
        return loadSnapshotByVersionId(pointerVersionId())
                .orElseGet(SanctionsSnapshotAdapter::emptySnapshot);
    }

    private Optional<UUID> pointerVersionId() {
        return currentListVersionRepository.findById(CURRENT_POINTER_ID)
                .map(CurrentListVersionEntity::getVersionId);
    }

    private Optional<SanctionsSet> loadSnapshotByVersionId(Optional<UUID> versionId) {
        return versionId.map(this::buildSnapshot);
    }

    private SanctionsSet buildSnapshot(UUID versionId) {
        var addresses = sanctionedAddressRepository.findByVersionId(versionId).stream()
                .map(SanctionedAddressEntity::getAddress)
                .collect(Collectors.toUnmodifiableSet());
        return SanctionsSet.builder()
                .versionId(versionId)
                .addresses(addresses)
                .loadedAt(Instant.now())
                .build();
    }

    private static SanctionsSet emptySnapshot() {
        return SanctionsSet.builder()
                .versionId(null)
                .addresses(Set.of())
                .loadedAt(Instant.now())
                .build();
    }
}
