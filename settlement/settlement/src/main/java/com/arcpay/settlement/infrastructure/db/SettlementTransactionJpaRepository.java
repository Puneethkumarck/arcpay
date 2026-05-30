package com.arcpay.settlement.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SettlementTransactionJpaRepository extends JpaRepository<SettlementTransactionEntity, UUID> {
}
