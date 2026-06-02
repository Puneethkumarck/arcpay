package com.arcpay.compliance.infrastructure.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScreeningCheckRepository extends JpaRepository<ScreeningCheckEntity, UUID> {

    List<ScreeningCheckEntity> findByScreeningId(UUID screeningId);
}
