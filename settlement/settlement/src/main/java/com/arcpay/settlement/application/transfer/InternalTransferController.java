package com.arcpay.settlement.application.transfer;

import com.arcpay.settlement.api.model.TransferInitiatedResponse;
import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.application.transfer.mapper.TransferInitiatedResponseMapper;
import com.arcpay.settlement.application.transfer.mapper.TransferRequestMapper;
import com.arcpay.settlement.domain.TransferSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/transfers")
@RequiredArgsConstructor
@Validated
public class InternalTransferController {

    private final TransferSubmissionService transferSubmissionService;
    private final TransferRequestMapper transferRequestMapper;
    private final TransferInitiatedResponseMapper transferInitiatedResponseMapper;

    @PostMapping
    public TransferInitiatedResponse submitTransfer(@Valid @RequestBody TransferRequest request) {
        log.info("Transfer submit accepted paymentId={}", request.paymentId());
        var submission = transferSubmissionService.submit(transferRequestMapper.toDomain(request));
        return transferInitiatedResponseMapper.toApi(request.paymentId(), submission);
    }
}
