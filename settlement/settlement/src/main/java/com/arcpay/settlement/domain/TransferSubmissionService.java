package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.model.TransferCommand;
import com.arcpay.settlement.domain.model.TransferSubmission;
import com.arcpay.settlement.domain.port.CustodyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferSubmissionService {

    private final CustodyProvider custodyProvider;

    public TransferSubmission submit(TransferCommand command) {
        log.info("Transfer submit requested paymentId={}", command.paymentId());
        return custodyProvider.submitTransfer(command);
    }
}
