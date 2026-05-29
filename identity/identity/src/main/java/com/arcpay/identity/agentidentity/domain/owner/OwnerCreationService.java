package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
import com.arcpay.identity.agentidentity.domain.model.OwnerWithApiKey;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class OwnerCreationService {

    private static final String API_KEY_PREFIX = "ak_test_";
    private static final int API_KEY_RANDOM_LENGTH = 32;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public OwnerWithApiKey createOwner(String email, String walletAddress) {
        var ownerId = UuidCreator.getTimeOrderedEpoch();
        var rawApiKey = generateApiKey();
        var apiKeyHash = hashApiKey(rawApiKey);
        var normalizedWallet = walletAddress.toLowerCase(Locale.ROOT);
        var now = Instant.now();

        var owner = Owner.builder()
                .ownerId(ownerId)
                .email(email)
                .walletAddress(normalizedWallet)
                .apiKeyHash(apiKeyHash)
                .status(OwnerStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return new OwnerWithApiKey(owner, rawApiKey);
    }

    String generateApiKey() {
        var sb = new StringBuilder(API_KEY_PREFIX);
        for (int i = 0; i < API_KEY_RANDOM_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    String hashApiKey(String rawApiKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
