package com.arcpay.settlement.domain.port;

import com.arcpay.settlement.domain.model.TransferCommand;
import com.arcpay.settlement.domain.model.TransferStatus;
import com.arcpay.settlement.domain.model.TransferSubmission;
import com.arcpay.settlement.domain.model.WalletBalance;

public interface CustodyProvider {

    TransferSubmission submitTransfer(TransferCommand command);

    TransferStatus getStatus(String circleTxId);

    WalletBalance getBalance(String walletId);
}
