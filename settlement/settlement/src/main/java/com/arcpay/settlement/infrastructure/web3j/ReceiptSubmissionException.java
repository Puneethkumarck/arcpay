package com.arcpay.settlement.infrastructure.web3j;

class ReceiptSubmissionException extends RuntimeException {

    ReceiptSubmissionException(String message) {
        super("On-chain receipt submission rejected: " + message);
    }
}
