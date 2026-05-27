package com.arcpay.identity.agentidentity.infrastructure.db.gasusage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface GasUsageJpaRepository extends JpaRepository<GasUsageEntity, UUID> {

    Page<GasUsageEntity> findByOwnerId(UUID ownerId, Pageable pageable);
}
