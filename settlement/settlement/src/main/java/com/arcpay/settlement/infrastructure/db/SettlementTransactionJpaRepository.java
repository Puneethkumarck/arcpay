package com.arcpay.settlement.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SettlementTransactionJpaRepository extends JpaRepository<SettlementTransactionEntity, UUID> {

    Optional<SettlementTransactionEntity> findByCircleTxId(String circleTxId);
}
