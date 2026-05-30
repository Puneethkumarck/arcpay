package com.arcpay.policy.policyengine.api;

public final class ErrorCodes {

    private static final String PREFIX = "ARCPAY-POLICY-";

    public static final String POLICY_NOT_FOUND = code(1);
    public static final String INVALID_POLICY = code(2);
    public static final String POLICY_VIOLATION = code(3);
    public static final String AGENT_NOT_ACTIVE = code(4);
    public static final String AGENT_NOT_FOUND = code(5);
    public static final String AGENT_OWNERSHIP = code(6);
    public static final String POLICY_HASH_MISMATCH = code(7);
    public static final String IDENTITY_UNAVAILABLE = code(8);
    public static final String RESERVATION_NOT_FOUND = code(9);
    public static final String ILLEGAL_RESERVATION_STATE = code(10);
    public static final String INTERNAL_ERROR = code(50);

    private ErrorCodes() {}

    public static String code(int number) {
        return PREFIX + String.format("%04d", number);
    }
}
