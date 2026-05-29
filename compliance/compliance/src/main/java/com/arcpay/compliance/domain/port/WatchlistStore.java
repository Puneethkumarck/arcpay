package com.arcpay.compliance.domain.port;

import java.util.Set;

public interface WatchlistStore {

    void addAddress(String normalizedAddress, String label, String addedBy);

    void removeAddress(String normalizedAddress);

    Set<String> getAllAddresses();

    boolean contains(String normalizedAddress);
}
