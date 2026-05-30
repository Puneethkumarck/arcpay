package com.arcpay.settlement.application.transfer.mapper;

import com.arcpay.settlement.api.model.TransferInitiatedResponse;
import com.arcpay.settlement.domain.model.TransferSubmission;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransferInitiatedResponseMapper {

    public TransferInitiatedResponse toApi(UUID paymentId, TransferSubmission submission) {
        return TransferInitiatedResponse.builder()
                .paymentId(paymentId)
                .circleTxId(submission.circleTxId())
                .state(submission.state().name())
                .build();
    }
}
