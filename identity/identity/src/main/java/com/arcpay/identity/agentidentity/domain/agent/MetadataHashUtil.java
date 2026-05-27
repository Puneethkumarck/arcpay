package com.arcpay.identity.agentidentity.domain.agent;

import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

public final class MetadataHashUtil {

    private MetadataHashUtil() {}

    public static String computeMetadataHash(String name, String purpose) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        var packed = (name + purpose).getBytes(StandardCharsets.UTF_8);
        var hash = Hash.sha3(packed);
        return "0x" + HexFormat.of().formatHex(hash);
    }
}
