package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import org.springframework.cloud.openfeign.FallbackFactory;

public class SettlementClientFallbackFactory implements FallbackFactory<SettlementServiceClient> {

    @Override
    public SettlementServiceClient create(Throwable cause) {
        return new FailingSettlementServiceClient(cause);
    }

    private record FailingSettlementServiceClient(Throwable cause) implements SettlementServiceClient {

        @Override
        public TransferStatusResponse submitTransfer(TransferRequest request) {
            throw mapFailure();
        }

        @Override
        public BalanceResponse balance(String agentId) {
            throw mapFailure();
        }

        @Override
        public void recordReceipt(ReceiptSubmissionRequest request) {
            throw mapFailure();
        }

        private RuntimeException mapFailure() {
            return new SettlementServiceCallException("Settlement service call failed", cause);
        }
    }
}
