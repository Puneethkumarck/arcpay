package com.arcpay.compliance.api;

public final class ErrorCodes {

    private static final String PREFIX = "ARCPAY-COMPLIANCE-";

    public static final String SCREENING_NOT_FOUND = code(1);
    public static final String HOLD_NOT_FOUND = code(2);
    public static final String NOT_AUTHORIZED = code(3);
    public static final String HOLD_ALREADY_DECIDED = code(4);
    public static final String REVIEW_REASON_INVALID = code(5);
    public static final String IDENTITY_UNAVAILABLE = code(6);
    public static final String MALFORMED_ADDRESS = code(7);
    public static final String MALFORMED_REQUEST = code(8);
    public static final String INTERNAL_ERROR = code(50);

    private ErrorCodes() {}

    public static String code(int number) {
        return PREFIX + String.format("%04d", number);
    }
}
