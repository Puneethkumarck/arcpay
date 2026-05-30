package com.arcpay.identity.agentidentity.fixtures;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

public final class CircleKeyFixtures {

    public static final String SOME_ENTITY_SECRET_HEX =
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private CircleKeyFixtures() {
    }

    public static String publicKeyPem() {
        return toPem(KEY_PAIR.getPublic());
    }

    public static String entityPublicKeyResponseJson() {
        return """
                {
                  "data": {
                    "publicKey": "%s"
                  }
                }
                """.formatted(publicKeyPem().replace("\n", "\\n"));
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
}
