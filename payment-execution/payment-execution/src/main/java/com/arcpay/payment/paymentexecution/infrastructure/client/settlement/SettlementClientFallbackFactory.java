package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.ReceiptRequest;
import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
class SettlementClientFallbackFactory implements FallbackFactory<SettlementServiceClient> {

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
        public void recordReceipt(ReceiptRequest request) {
            throw mapFailure();
        }

        private RuntimeException mapFailure() {
            if (cause instanceof FeignException feignException && isClientError(feignException)) {
                return feignException;
            }
            return new SettlementServiceCallException("Settlement service call failed", cause);
        }

        private boolean isClientError(FeignException feignException) {
            return feignException.status() >= 400 && feignException.status() < 500;
        }
    }
}
