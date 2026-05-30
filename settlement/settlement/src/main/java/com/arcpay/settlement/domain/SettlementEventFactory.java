package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import com.arcpay.settlement.domain.model.SettlementTransaction;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.arcpay.settlement.domain.model.TransferState.CANCELLED;
import static com.arcpay.settlement.domain.model.TransferState.COMPLETED;
import static com.arcpay.settlement.domain.model.TransferState.CONFIRMED;
import static com.arcpay.settlement.domain.model.TransferState.DENIED;
import static com.arcpay.settlement.domain.model.TransferState.FAILED;
import static com.arcpay.settlement.domain.model.TransferState.INITIATED;
import static com.arcpay.settlement.domain.model.TransferState.QUEUED;
import static com.arcpay.settlement.domain.model.TransferState.SENT;
import static com.arcpay.settlement.domain.model.TransferState.STUCK;

@Service
public class SettlementEventFactory {

    public Optional<Object> eventFor(SettlementTransaction transaction) {
        return switch (transaction.state()) {
            case COMPLETED -> Optional.of(toConfirmed(transaction));
            case FAILED, DENIED, CANCELLED -> Optional.of(toReverted(transaction));
            case INITIATED, QUEUED, SENT, CONFIRMED, STUCK -> Optional.empty();
        };
    }

    private TransferConfirmed toConfirmed(SettlementTransaction transaction) {
        return TransferConfirmed.builder()
                .paymentId(transaction.paymentId())
                .txHash(transaction.txHash())
                .networkFee(transaction.networkFee())
                .confirmedAt(Instant.now())
                .build();
    }

    private TransferReverted toReverted(SettlementTransaction transaction) {
        return TransferReverted.builder()
                .paymentId(transaction.paymentId())
                .reason(reasonFor(transaction))
                .revertedAt(Instant.now())
                .build();
    }

    private String reasonFor(SettlementTransaction transaction) {
        return transaction.errorReason() != null
                ? transaction.errorReason()
                : transaction.state().name();
    }
}
