package com.arcpay.identity.agentidentity.domain.agent;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts a Java UUID to a Solidity bytes32 value (left-padded with zeros).
 * UUID occupies positions 16–31, positions 0–15 are zero.
 */
public final class UuidConversionUtil {

    private static final int BYTES32_LENGTH = 32;
    private static final int UUID_BYTE_LENGTH = 16;

    private UuidConversionUtil() {}

    public static byte[] uuidToBytes32(UUID uuid) {
        var bytes32 = new byte[BYTES32_LENGTH];
        var buffer = ByteBuffer.wrap(bytes32, BYTES32_LENGTH - UUID_BYTE_LENGTH, UUID_BYTE_LENGTH);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return bytes32;
    }

    public static UUID bytes32ToUuid(byte[] bytes32) {
        if (bytes32.length != BYTES32_LENGTH) {
            throw new IllegalArgumentException("Expected 32 bytes but got " + bytes32.length);
        }
        var buffer = ByteBuffer.wrap(bytes32, BYTES32_LENGTH - UUID_BYTE_LENGTH, UUID_BYTE_LENGTH);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
