package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SanctionsSnapshotWriter {

    void persistSnapshot(UUID versionId, String checksum,
                         Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource);

    void flipCurrentVersion(UUID versionId);
}
