package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SanctionsListVersionRepository extends JpaRepository<SanctionsListVersionEntity, UUID> {

    Optional<SanctionsListVersionEntity> findFirstByStatusOrderByDownloadedAtDesc(String status);
}
