package com.arcpay.settlement.infrastructure.db;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SettlementTransactionRepositoryAdapter implements SettlementTransactionRepository {

    private final SettlementTransactionJpaRepository jpaRepository;
    private final SettlementTransactionEntityMapper mapper;

    @Override
    public SettlementTransaction save(SettlementTransaction transaction) {
        var existing = jpaRepository.findById(transaction.paymentId());
        if (existing.isPresent()) {
            return mapper.mapToDomain(existing.get());
        }
        return mapper.mapToDomain(jpaRepository.saveAndFlush(mapper.mapToEntity(transaction)));
    }

    @Override
    public Optional<SettlementTransaction> findByPaymentId(UUID paymentId) {
        return jpaRepository.findById(paymentId).map(mapper::mapToDomain);
    }
}
