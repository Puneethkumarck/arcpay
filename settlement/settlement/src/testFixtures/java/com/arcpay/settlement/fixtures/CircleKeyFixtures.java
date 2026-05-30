package com.arcpay.settlement.fixtures;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public final class CircleKeyFixtures {

    public static final String SOME_ENTITY_SECRET_HEX =
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";

    public static final String SOME_KEY_ID = "circle-webhook-key-001";

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final KeyPair EC_KEY_PAIR = generateEcKeyPair();

    private CircleKeyFixtures() {
    }

    public static PublicKey publicKey() {
        return KEY_PAIR.getPublic();
    }

    public static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    public static String publicKeyPem() {
        return toPem(KEY_PAIR.getPublic());
    }

    public static String webhookPublicKeyPem() {
        var base64 = Base64.getEncoder().encodeToString(EC_KEY_PAIR.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----" + base64 + "-----END PUBLIC KEY-----";
    }

    public static String signWebhook(String body) {
        try {
            var signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(EC_KEY_PAIR.getPrivate());
            signer.update(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign webhook body for tests", e);
        }
    }

    private static String toPem(PublicKey key) {
        var base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair for tests", e);
        }
    }

    private static KeyPair generateEcKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate EC key pair for tests", e);
        }
    }
}
