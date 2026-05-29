package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.api.model.WatchlistEntryRequest;
import com.arcpay.compliance.api.model.WatchlistEntryResponse;
import com.arcpay.compliance.domain.exception.MalformedAddressException;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.platform.api.OwnerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/compliance/watchlist")
@RequiredArgsConstructor
@Validated
public class WatchlistController {

    private static final Pattern EVM_ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private final WatchlistStore watchlistStore;

    @PostMapping
    public ResponseEntity<WatchlistEntryResponse> addAddress(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody WatchlistEntryRequest request) {
        var normalized = normalizeOrReject(request.address());
        log.info("Watchlist add requested address={} addedBy={}", normalized, principal.email());
        watchlistStore.addAddress(normalized, request.label(), principal.email());
        return ResponseEntity.status(CREATED).body(WatchlistEntryResponse.builder()
                .address(normalized)
                .label(request.label())
                .build());
    }

    @DeleteMapping("/{address}")
    public ResponseEntity<Void> removeAddress(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable String address) {
        var normalized = normalizeOrReject(address);
        log.info("Watchlist remove requested address={} removedBy={}", normalized, principal.email());
        watchlistStore.removeAddress(normalized);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<WatchlistEntryResponse> listAddresses() {
        return watchlistStore.getAllAddresses().stream()
                .sorted()
                .map(address -> WatchlistEntryResponse.builder().address(address).build())
                .toList();
    }

    private static String normalizeOrReject(String address) {
        if (address == null || !EVM_ADDRESS.matcher(address.trim()).matches()) {
            throw new MalformedAddressException(address);
        }
        return address.trim().toLowerCase(Locale.ROOT);
    }
}
