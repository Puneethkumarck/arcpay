package com.arcpay.payment.paymentexecution.api;

public final class ErrorCodes {

    private static final String PREFIX = "ARCPAY-PAYMENT-";

    public static final String PAYMENT_NOT_FOUND = code(1);
    public static final String PAYMENT_ACCESS_DENIED = code(2);
    public static final String INVALID_PAYMENT_REQUEST = code(3);
    public static final String AGENT_NOT_ACTIVE = code(4);
    public static final String IDEMPOTENCY_CONFLICT = code(5);
    public static final String POLICY_UNAVAILABLE = code(6);
    public static final String SETTLEMENT_UNAVAILABLE = code(7);
    public static final String IDENTITY_UNAVAILABLE = code(8);
    public static final String INTERNAL_ERROR = code(50);

    private ErrorCodes() {}

    public static String code(int number) {
        return PREFIX + String.format("%04d", number);
    }
}
