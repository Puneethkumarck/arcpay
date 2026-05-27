package com.arcpay.identity.agentidentity.domain.agent;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidConversionUtilTest {

    @Test
    void shouldProduceThirtyTwoBytesLeftPadded() {
        // given
        var uuid = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

        // when
        var result = UuidConversionUtil.uuidToBytes32(uuid);

        // then
        assertThat(result).hasSize(32);
        // First 16 bytes should be zero (left-padded)
        for (int i = 0; i < 16; i++) {
            assertThat(result[i]).isZero();
        }
    }

    @Test
    void shouldPlaceUuidBytesInPositions16Through31() {
        // given
        var uuid = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

        // when
        var result = UuidConversionUtil.uuidToBytes32(uuid);

        // then — at least some of the UUID bytes should be non-zero
        var hasNonZero = false;
        for (int i = 16; i < 32; i++) {
            if (result[i] != 0) {
                hasNonZero = true;
                break;
            }
        }
        assertThat(hasNonZero).isTrue();
    }

    @Test
    void shouldRoundTripPreserveUuidValue() {
        // given
        var uuid = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");

        // when
        var bytes32 = UuidConversionUtil.uuidToBytes32(uuid);
        var roundTripped = UuidConversionUtil.bytes32ToUuid(bytes32);

        // then
        assertThat(roundTripped).isEqualTo(uuid);
    }

    @Test
    void shouldRejectWrongLengthBytes() {
        // given
        var tooShort = new byte[16];

        // when / then
        assertThatThrownBy(() -> UuidConversionUtil.bytes32ToUuid(tooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }

    @Test
    void shouldHandleNilUuid() {
        // given
        var nilUuid = new UUID(0L, 0L);

        // when
        var result = UuidConversionUtil.uuidToBytes32(nilUuid);

        // then
        assertThat(result).containsOnly(0);
    }
}
