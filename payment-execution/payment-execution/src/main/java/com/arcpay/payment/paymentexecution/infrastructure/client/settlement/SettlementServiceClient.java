package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.settlement.api.model.BalanceResponse;
import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.api.model.TransferStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "settlement-service",
        url = "${arcpay.settlement-service.url}",
        fallbackFactory = SettlementClientFallbackFactory.class)
public interface SettlementServiceClient {

    @PostMapping("/api/v1/internal/transfers")
    TransferStatusResponse submitTransfer(@RequestBody TransferRequest request);

    @GetMapping("/api/v1/internal/wallets/{agentId}/balance")
    BalanceResponse balance(@PathVariable String agentId);

    @PostMapping("/api/v1/internal/receipts")
    void recordReceipt(@RequestBody ReceiptSubmissionRequest request);
}
