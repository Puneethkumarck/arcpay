package com.arcpay.settlement.infrastructure.circle;

class CircleApiException extends RuntimeException {

    CircleApiException(String message) {
        super(message);
    }

    CircleApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
