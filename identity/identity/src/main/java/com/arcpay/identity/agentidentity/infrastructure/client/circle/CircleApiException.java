package com.arcpay.identity.agentidentity.infrastructure.client.circle;

class CircleApiException extends RuntimeException {

    CircleApiException(String message) {
        super(message);
    }

    CircleApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
