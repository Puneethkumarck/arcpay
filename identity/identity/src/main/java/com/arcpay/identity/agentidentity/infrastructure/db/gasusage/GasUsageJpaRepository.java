package com.arcpay.identity.agentidentity.infrastructure.db.gasusage;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface GasUsageJpaRepository extends JpaRepository<GasUsageEntity, UUID> {

    Page<GasUsageEntity> findByOwnerId(UUID ownerId, Pageable pageable);
}
