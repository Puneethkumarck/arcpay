package com.arcpay.identity.agentidentity.api;

public final class ErrorCodes {

    private static final String PREFIX = "ARCPAY-IDENTITY-";

    public static final String BAD_REQUEST = code(1);
    public static final String NOT_FOUND = code(2);
    public static final String FORBIDDEN = code(3);
    public static final String CONFLICT = code(4);
    public static final String UNAUTHORIZED = code(5);
    public static final String IDEMPOTENCY = code(6);
    public static final String TOO_MANY_REQUESTS = code(7);
    public static final String INTERNAL_ERROR = code(50);
    public static final String EXTERNAL_SERVICE_ERROR = code(51);

    private ErrorCodes() {}

    public static String code(int number) {
        return PREFIX + String.format("%04d", number);
    }
}
