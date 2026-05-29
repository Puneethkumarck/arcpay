package com.arcpay.compliance.domain.exception;

public class MalformedAddressException extends RuntimeException {

    public MalformedAddressException(String address) {
        super("Malformed EVM address: " + address);
    }
}
