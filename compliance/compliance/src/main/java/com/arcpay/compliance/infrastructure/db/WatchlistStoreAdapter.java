package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.port.WatchlistStore;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class WatchlistStoreAdapter implements WatchlistStore {

    private final WatchlistAddressRepository watchlistAddressRepository;

    @Override
    @Transactional
    public void addAddress(String normalizedAddress, String label, String addedBy) {
        var address = normalize(normalizedAddress);
        if (watchlistAddressRepository.existsByAddress(address)) {
            return;
        }
        try {
            watchlistAddressRepository.save(WatchlistAddressEntity.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .address(address)
                    .label(label)
                    .addedBy(addedBy)
                    .addedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            return;
        }
    }

    @Override
    @Transactional
    public void removeAddress(String normalizedAddress) {
        watchlistAddressRepository.deleteByAddress(normalize(normalizedAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllAddresses() {
        return watchlistAddressRepository.findAll().stream()
                .map(WatchlistAddressEntity::getAddress)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(String normalizedAddress) {
        return watchlistAddressRepository.existsByAddress(normalize(normalizedAddress));
    }

    private static String normalize(String address) {
        return address.toLowerCase(Locale.ROOT);
    }
}
