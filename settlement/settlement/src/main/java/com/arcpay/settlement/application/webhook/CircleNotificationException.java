package com.arcpay.settlement.application.webhook;

public class CircleNotificationException extends RuntimeException {

    CircleNotificationException(String message) {
        super(message);
    }

    CircleNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
