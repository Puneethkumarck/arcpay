package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferNotification;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.EventPublisher;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.arcpay.settlement.domain.model.TransferState.CANCELLED;
import static com.arcpay.settlement.domain.model.TransferState.COMPLETED;
import static com.arcpay.settlement.domain.model.TransferState.DENIED;
import static com.arcpay.settlement.domain.model.TransferState.FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferNotificationHandler {

    private static final Set<TransferState> TERMINAL = Set.of(COMPLETED, FAILED, DENIED, CANCELLED);

    private final SettlementTransactionRepository repository;
    private final SettlementEventFactory eventFactory;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(TransferNotification notification) {
        var transaction = repository.findByCircleTxId(notification.circleTxId())
                .orElseThrow(() -> new TransferNotFoundException(
                        "No settlement_transaction for circleTxId=" + notification.circleTxId()));

        if (TERMINAL.contains(transaction.state())) {
            log.info("Duplicate notification ignored paymentId={} circleTxId={} state={}",
                    transaction.paymentId(), notification.circleTxId(), transaction.state());
            return;
        }

        var updated = apply(transaction, notification);
        repository.update(updated);

        eventFactory.eventFor(updated).ifPresent(eventPublisher::publish);
    }

    private SettlementTransaction apply(SettlementTransaction transaction, TransferNotification notification) {
        var builder = transaction.toBuilder()
                .state(notification.state())
                .updatedAt(java.time.Instant.now());
        if (notification.txHash() != null) {
            builder.txHash(notification.txHash());
        }
        if (notification.networkFee() != null) {
            builder.networkFee(notification.networkFee());
        }
        if (notification.errorReason() != null) {
            builder.errorReason(notification.errorReason());
        }
        return builder.build();
    }
}
