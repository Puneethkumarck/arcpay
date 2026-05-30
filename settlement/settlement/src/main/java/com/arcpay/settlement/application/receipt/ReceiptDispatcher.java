package com.arcpay.settlement.application.receipt;

import com.arcpay.settlement.domain.model.ReceiptCommand;
import com.arcpay.settlement.domain.port.ReceiptWriter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
class ReceiptDispatcher {

    private final ObjectProvider<ReceiptWriter> receiptWriter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    ReceiptDispatcher(ObjectProvider<ReceiptWriter> receiptWriter) {
        this.receiptWriter = receiptWriter;
    }

    void dispatch(ReceiptCommand command) {
        var writer = receiptWriter.getIfAvailable();
        if (writer == null) {
            log.warn("No ReceiptWriter configured; skipping on-chain receipt paymentId={}", command.paymentId());
            return;
        }
        executor.execute(() -> writer.writeReceipt(command));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
