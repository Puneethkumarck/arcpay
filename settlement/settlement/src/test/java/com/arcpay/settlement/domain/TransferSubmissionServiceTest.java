package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.model.TransferSubmission;
import com.arcpay.settlement.domain.port.CustodyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someTransferCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TransferSubmissionServiceTest {

    @Mock
    private CustodyProvider custodyProvider;

    @InjectMocks
    private TransferSubmissionService service;

    @Test
    void shouldSubmitFreshTransferAndReturnInitiatedSubmission() {
        // given
        var command = someTransferCommand();
        var submission = new TransferSubmission(SOME_CIRCLE_TX_ID, TransferState.INITIATED);
        given(custodyProvider.submitTransfer(command)).willReturn(submission);

        // when
        var result = service.submit(command);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(submission);
        then(custodyProvider).should().submitTransfer(command);
    }

    @Test
    void shouldReturnExistingSubmissionFromCustodyOnReplay() {
        // given
        var command = someTransferCommand();
        var existing = new TransferSubmission(SOME_CIRCLE_TX_ID, TransferState.INITIATED);
        given(custodyProvider.submitTransfer(command)).willReturn(existing);

        // when
        var result = service.submit(command);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existing);
    }
}
