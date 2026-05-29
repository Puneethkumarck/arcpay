package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ScreeningResultRepository extends JpaRepository<ScreeningResultEntity, UUID> {

    Optional<ScreeningResultEntity> findByPaymentId(UUID paymentId);

    List<ScreeningResultEntity> findByAgentIdOrderByScreenedAtDesc(UUID agentId);
}
