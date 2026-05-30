package com.arcpay.settlement.domain.port;

public interface WebhookSignatureVerifier {

    void verify(String body, String keyId, String signature);
}
