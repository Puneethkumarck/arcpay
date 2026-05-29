package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ScreeningCheckRepository extends JpaRepository<ScreeningCheckEntity, UUID> {

    List<ScreeningCheckEntity> findByScreeningId(UUID screeningId);
}
