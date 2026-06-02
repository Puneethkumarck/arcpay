package com.arcpay.compliance.infrastructure.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CurrentListVersionRepository extends JpaRepository<CurrentListVersionEntity, Short> {

    Optional<CurrentListVersionEntity> findById(Short id);
}
