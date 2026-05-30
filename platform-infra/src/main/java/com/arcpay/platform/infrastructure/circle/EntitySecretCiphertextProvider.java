package com.arcpay.platform.infrastructure.circle;

import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

public class EntitySecretCiphertextProvider {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int ENTITY_SECRET_BYTES = 32;
    private static final String PEM_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_FOOTER = "-----END PUBLIC KEY-----";

    private final String entitySecretHex;
    private final RestClient restClient;
    private final AtomicReference<PublicKey> cachedPublicKey = new AtomicReference<>();

    public EntitySecretCiphertextProvider(String entitySecretHex, RestClient circleRestClient) {
        this.entitySecretHex = entitySecretHex;
        this.restClient = circleRestClient;
    }

    public String generate() {
        var entitySecret = decodeEntitySecret();
        try {
            var spec = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    new MGF1ParameterSpec("SHA-256"),
                    PSource.PSpecified.DEFAULT);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, circlePublicKey(), spec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(entitySecret));
        } catch (Exception e) {
            throw new CircleEntitySecretException("Failed to build entity-secret ciphertext", e);
        }
    }

    private byte[] decodeEntitySecret() {
        byte[] secret;
        try {
            secret = HexFormat.of().parseHex(entitySecretHex);
        } catch (IllegalArgumentException e) {
            throw new CircleEntitySecretException("Entity secret must be a 32-byte hex string", e);
        }
        if (secret.length != ENTITY_SECRET_BYTES) {
            throw new CircleEntitySecretException("Entity secret must decode to 32 bytes");
        }
        return secret;
    }

    private PublicKey circlePublicKey() {
        var cached = cachedPublicKey.get();
        if (cached != null) {
            return cached;
        }
        var fetched = fetchPublicKey();
        cachedPublicKey.compareAndSet(null, fetched);
        return cachedPublicKey.get();
    }

    private PublicKey fetchPublicKey() {
        var response = restClient.get()
                .uri("/v1/w3s/config/entity/publicKey")
                .retrieve()
                .body(PublicKeyResponse.class);

        if (response == null || response.data() == null || response.data().publicKey() == null) {
            throw new CircleEntitySecretException("Empty entity public key response from Circle API");
        }

        return parsePublicKey(response.data().publicKey());
    }

    private PublicKey parsePublicKey(String pem) {
        var base64 = pem
                .replace(PEM_HEADER, "")
                .replace(PEM_FOOTER, "")
                .replaceAll("\\s", "");
        try {
            var decoded = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new CircleEntitySecretException("Failed to parse Circle entity public key", e);
        }
    }

    record PublicKeyResponse(PublicKeyData data) {
        record PublicKeyData(String publicKey) {}
    }
}
