package com.arcpay.settlement.domain.port;

import com.arcpay.settlement.domain.model.ReceiptCommand;

public interface ReceiptWriter {

    String writeReceipt(ReceiptCommand command);
}
