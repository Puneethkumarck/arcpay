package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SanctionedAddressRepository extends JpaRepository<SanctionedAddressEntity, UUID> {

    List<SanctionedAddressEntity> findByVersionId(UUID versionId);

    boolean existsByVersionIdAndAddress(UUID versionId, String address);
}
