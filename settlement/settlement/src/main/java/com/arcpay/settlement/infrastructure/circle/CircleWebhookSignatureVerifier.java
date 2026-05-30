package com.arcpay.settlement.infrastructure.circle;

import com.arcpay.settlement.domain.WebhookSignatureException;
import com.arcpay.settlement.domain.port.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
class CircleWebhookSignatureVerifier implements WebhookSignatureVerifier {

    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";

    private final RestClient restClient;
    private final ConcurrentHashMap<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    @Override
    public void verify(String body, String keyId, String signature) {
        if (keyId == null || keyId.isBlank()) {
            throw new WebhookSignatureException("Missing X-Circle-Key-Id header");
        }
        if (signature == null || signature.isBlank()) {
            throw new WebhookSignatureException("Missing X-Circle-Signature header");
        }

        var publicKey = keyCache.computeIfAbsent(keyId, this::fetchPublicKey);

        if (!isValid(body, signature, publicKey)) {
            throw new WebhookSignatureException("Invalid Circle webhook signature for keyId=" + keyId);
        }
    }

    private boolean isValid(String body, String signature, PublicKey publicKey) {
        try {
            var verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(body.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            throw new WebhookSignatureException("Failed to verify Circle webhook signature", e);
        }
    }

    private PublicKey fetchPublicKey(String keyId) {
        try {
            var response = restClient.get()
                    .uri("/v2/notifications/publicKey/{keyId}", keyId)
                    .retrieve()
                    .body(PublicKeyResponse.class);

            if (response == null || response.data() == null || response.data().publicKey() == null) {
                throw new WebhookSignatureException("Empty public key response from Circle for keyId=" + keyId);
            }
            return parsePublicKey(response.data().publicKey());
        } catch (WebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookSignatureException("Failed to fetch Circle public key for keyId=" + keyId, e);
        }
    }

    private PublicKey parsePublicKey(String pem) {
        try {
            var base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            var keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64));
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (Exception e) {
            throw new WebhookSignatureException("Failed to parse Circle EC public key", e);
        }
    }

    record PublicKeyResponse(Data data) {
        record Data(String id, String publicKey) {}
    }
}
