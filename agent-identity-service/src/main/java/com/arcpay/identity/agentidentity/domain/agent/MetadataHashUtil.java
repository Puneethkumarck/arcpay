package com.arcpay.identity.agentidentity.domain.agent;

import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Computes keccak256(abi.encodePacked(name, purpose)) to match the on-chain
 * AgentRegistry.sol metadataHash computation.
 */
public final class MetadataHashUtil {

    private MetadataHashUtil() {}

    public static String computeMetadataHash(String name, String purpose) {
        // abi.encodePacked concatenates raw bytes without padding
        var packed = (name + purpose).getBytes(StandardCharsets.UTF_8);
        var hash = Hash.sha3(packed);
        return "0x" + HexFormat.of().formatHex(hash);
    }
}
