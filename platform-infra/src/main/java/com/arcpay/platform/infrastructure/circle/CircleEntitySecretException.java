package com.arcpay.platform.infrastructure.circle;

public class CircleEntitySecretException extends RuntimeException {

    public CircleEntitySecretException(String message) {
        super(message);
    }

    public CircleEntitySecretException(String message, Throwable cause) {
        super(message, cause);
    }
}
