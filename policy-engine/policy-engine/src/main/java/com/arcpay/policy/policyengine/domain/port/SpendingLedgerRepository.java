package com.arcpay.policy.policyengine.domain.port;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;

import java.util.Optional;
import java.util.UUID;

public interface SpendingLedgerRepository {

    SpendingLedgerEntry save(SpendingLedgerEntry entry);

    Optional<SpendingLedgerEntry> findByPaymentId(UUID paymentId);

    SpendingSummary getSpendingSummary(UUID agentId, int velocityMinutes);
}
