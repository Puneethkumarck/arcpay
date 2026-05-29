package com.arcpay.compliance.infrastructure.sanctions;

import lombok.Builder;

import java.util.Objects;

@Builder(toBuilder = true)
public record SanctionedAddressRecord(
        String address,
        SanctionsSource source,
        String sourceRef
) {

    public SanctionedAddressRecord {
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
