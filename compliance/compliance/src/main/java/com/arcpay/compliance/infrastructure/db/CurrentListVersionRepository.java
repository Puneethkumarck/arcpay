package com.arcpay.compliance.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CurrentListVersionRepository extends JpaRepository<CurrentListVersionEntity, Short> {

    Optional<CurrentListVersionEntity> findById(Short id);
}
