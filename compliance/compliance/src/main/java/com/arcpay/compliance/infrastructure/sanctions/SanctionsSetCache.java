package com.arcpay.compliance.infrastructure.sanctions;

import com.arcpay.compliance.domain.model.SanctionsSet;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Primary
@Component
class SanctionsSetCache implements SanctionsSetProvider {

    private final SanctionsSetProvider snapshotSource;
    private final AtomicReference<SanctionsSet> cache = new AtomicReference<>();

    SanctionsSetCache(@Qualifier("sanctionsSnapshotAdapter") SanctionsSetProvider snapshotSource) {
        this.snapshotSource = snapshotSource;
    }

    @PostConstruct
    void initialize() {
        refresh();
    }

    @Override
    public SanctionsSet getCurrentSanctionsSet() {
        return cache.get();
    }

    boolean contains(String normalizedAddress) {
        return cache.get().contains(normalizedAddress);
    }

    @Scheduled(fixedDelayString = "${compliance.sanctions.poll-interval-ms}")
    void refresh() {
        var latest = snapshotSource.getCurrentSanctionsSet();
        var current = cache.get();
        if (current == null || !Objects.equals(current.versionId(), latest.versionId())) {
            cache.set(latest);
            log.info("Sanctions set swapped to version {} with {} addresses",
                    latest.versionId(), latest.addresses().size());
        }
    }
}
