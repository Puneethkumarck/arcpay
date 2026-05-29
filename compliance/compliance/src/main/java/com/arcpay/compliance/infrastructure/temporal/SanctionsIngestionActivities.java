package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ActivityInterface
public interface SanctionsIngestionActivities {

    @ActivityMethod
    byte[] downloadSource(SanctionsSource source);

    @ActivityMethod
    List<SanctionedAddressRecord> parseAddresses(SanctionsSource source, byte[] rawData);

    @ActivityMethod
    UUID validateSnapshot(Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource);

    @ActivityMethod
    void persistSnapshot(UUID versionId, Map<SanctionsSource, List<SanctionedAddressRecord>> recordsBySource);

    @ActivityMethod
    void flipCurrentVersion(UUID versionId);
}
