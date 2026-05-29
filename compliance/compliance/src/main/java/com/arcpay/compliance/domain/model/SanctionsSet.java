package com.arcpay.compliance.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Builder(toBuilder = true)
public record SanctionsSet(
        UUID versionId,
        Set<String> addresses,
        Instant loadedAt
) {

    public SanctionsSet {
        Objects.requireNonNull(addresses, "addresses must not be null");
        addresses = Set.copyOf(addresses);
    }

    public boolean contains(String normalizedAddress) {
        return addresses.contains(normalizedAddress);
    }
}
