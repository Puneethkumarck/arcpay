package com.arcpay.settlement.infrastructure.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementTransactionJpaRepository extends JpaRepository<SettlementTransactionEntity, UUID> {

    Optional<SettlementTransactionEntity> findByCircleTxId(String circleTxId);
}
