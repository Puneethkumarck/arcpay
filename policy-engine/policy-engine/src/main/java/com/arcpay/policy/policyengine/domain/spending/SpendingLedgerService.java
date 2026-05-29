package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import com.arcpay.policy.policyengine.domain.port.SpendingLedgerRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpendingLedgerService {

    private final SpendingLedgerRepository spendingLedgerRepository;

    @Transactional
    public SpendingLedgerEntry recordSpending(UUID agentId, UUID paymentId,
            BigDecimal amount, String recipient, Instant executedAt) {
        var existing = spendingLedgerRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.debug("Spending already recorded for paymentId={}, returning existing entry", paymentId);
            return existing.get();
        }

        var entry = SpendingLedgerEntry.builder()
                .entryId(UuidCreator.getTimeOrderedEpoch())
                .agentId(agentId)
                .paymentId(paymentId)
                .amount(amount)
                .recipient(recipient)
                .executedAt(executedAt)
                .createdAt(Instant.now())
                .build();

        var saved = spendingLedgerRepository.save(entry);
        log.debug("Recorded spending entryId={} for agentId={} paymentId={}",
                saved.entryId(), agentId, paymentId);
        return saved;
    }

    public SpendingSummary getSpendingSummary(UUID agentId, int velocityMinutes) {
        return spendingLedgerRepository.getSpendingSummary(agentId, velocityMinutes);
    }
}
