package com.arcpay.settlement.domain.port;

import com.arcpay.settlement.domain.model.SettlementTransaction;

import java.util.Optional;
import java.util.UUID;

public interface SettlementTransactionRepository {

    SettlementTransaction save(SettlementTransaction transaction);

    Optional<SettlementTransaction> findByPaymentId(UUID paymentId);
}
