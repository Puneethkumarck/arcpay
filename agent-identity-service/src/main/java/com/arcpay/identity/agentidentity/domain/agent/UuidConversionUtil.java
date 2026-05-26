package com.arcpay.identity.agentidentity.domain.agent;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public final class UuidConversionUtil {

    private static final int BYTES32_LENGTH = 32;
    private static final int UUID_BYTE_LENGTH = 16;

    private UuidConversionUtil() {}

    public static byte[] uuidToBytes32(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        var bytes32 = new byte[BYTES32_LENGTH];
        var buffer = ByteBuffer.wrap(bytes32, BYTES32_LENGTH - UUID_BYTE_LENGTH, UUID_BYTE_LENGTH);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return bytes32;
    }

    public static UUID bytes32ToUuid(byte[] bytes32) {
        Objects.requireNonNull(bytes32, "bytes32 must not be null");
        if (bytes32.length != BYTES32_LENGTH) {
            throw new IllegalArgumentException("Expected 32 bytes but got " + bytes32.length);
        }
        for (int i = 0; i < BYTES32_LENGTH - UUID_BYTE_LENGTH; i++) {
            if (bytes32[i] != 0) {
                throw new IllegalArgumentException(
                        "Non-canonical bytes32: expected zero padding in positions 0-15 but found non-zero byte at index " + i);
            }
        }
        var buffer = ByteBuffer.wrap(bytes32, BYTES32_LENGTH - UUID_BYTE_LENGTH, UUID_BYTE_LENGTH);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
