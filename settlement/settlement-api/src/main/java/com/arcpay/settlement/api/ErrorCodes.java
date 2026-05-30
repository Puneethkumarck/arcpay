package com.arcpay.settlement.api;

public final class ErrorCodes {

    private static final String PREFIX = "ARCPAY-SETTLEMENT-";

    public static final String TRANSFER_NOT_FOUND = code(1);
    public static final String INSUFFICIENT_BALANCE = code(2);
    public static final String CIRCLE_API_ERROR = code(3);
    public static final String INVALID_REQUEST = code(4);
    public static final String INTERNAL_ERROR = code(50);

    private ErrorCodes() {}

    public static String code(int number) {
        return PREFIX + String.format("%04d", number);
    }
}
