package com.arcpay.settlement.application.receipt;

import com.arcpay.settlement.api.model.ReceiptRequest;
import com.arcpay.settlement.application.receipt.mapper.ReceiptRequestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.ACCEPTED;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/receipts")
@RequiredArgsConstructor
@Validated
public class InternalReceiptController {

    private final ReceiptDispatcher receiptDispatcher;
    private final ReceiptRequestMapper receiptRequestMapper;

    @PostMapping
    public ResponseEntity<Void> recordReceipt(@Valid @RequestBody ReceiptRequest request) {
        log.info("Receipt write accepted paymentId={}", request.paymentId());
        receiptDispatcher.dispatch(receiptRequestMapper.toDomain(request));
        return ResponseEntity.status(ACCEPTED).build();
    }
}
