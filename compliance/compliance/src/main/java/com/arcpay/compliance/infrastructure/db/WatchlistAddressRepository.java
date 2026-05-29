package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface WatchlistAddressRepository extends JpaRepository<WatchlistAddressEntity, UUID> {

    Optional<WatchlistAddressEntity> findByAddress(String address);

    boolean existsByAddress(String address);

    void deleteByAddress(String address);
}
