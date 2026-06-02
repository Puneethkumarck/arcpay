package com.arcpay.compliance.infrastructure.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SanctionsListVersionRepository extends JpaRepository<SanctionsListVersionEntity, UUID> {

    Optional<SanctionsListVersionEntity> findFirstByStatusOrderByDownloadedAtDesc(String status);
}
