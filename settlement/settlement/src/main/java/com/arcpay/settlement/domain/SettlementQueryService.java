package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.WalletBalance;
import com.arcpay.settlement.domain.port.CustodyProvider;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementQueryService {

    private final SettlementTransactionRepository repository;
    private final CustodyProvider custodyProvider;

    @Transactional(readOnly = true)
    public SettlementTransaction findTransfer(UUID paymentId) {
        return repository.findByPaymentId(paymentId)
                .orElseThrow(() -> new TransferNotFoundException(
                        "Settlement transaction not found for paymentId=" + paymentId));
    }

    public WalletBalance balanceFor(String walletId) {
        return custodyProvider.getBalance(walletId);
    }
}
