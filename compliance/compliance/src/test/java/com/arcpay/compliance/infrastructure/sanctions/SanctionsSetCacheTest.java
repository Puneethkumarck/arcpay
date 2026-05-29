package com.arcpay.compliance.infrastructure.sanctions;

import com.arcpay.compliance.domain.model.SanctionsSet;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SanctionsSetCacheTest {

    private static final UUID VERSION_ONE = UUID.fromString("0197aa00-aaaa-7def-8000-aaaaaaaaaaaa");
    private static final UUID VERSION_TWO = UUID.fromString("0197aa00-bbbb-7def-8000-bbbbbbbbbbbb");

    private static final SanctionsSet SNAPSHOT_ONE = SanctionsSet.builder()
            .versionId(VERSION_ONE)
            .addresses(Set.of("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
            .loadedAt(Instant.parse("2026-06-01T00:00:00Z"))
            .build();

    private static final SanctionsSet SNAPSHOT_TWO = SanctionsSet.builder()
            .versionId(VERSION_TWO)
            .addresses(Set.of("0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
            .loadedAt(Instant.parse("2026-06-01T01:00:00Z"))
            .build();

    @Mock
    private SanctionsSetProvider snapshotSource;

    @InjectMocks
    private SanctionsSetCache cache;

    @Test
    void shouldInitializeWithCurrentSnapshot() {
        // given
        given(snapshotSource.getCurrentSanctionsSet()).willReturn(SNAPSHOT_ONE);

        // when
        cache.initialize();

        // then
        assertThat(cache.getCurrentSanctionsSet()).usingRecursiveComparison().isEqualTo(SNAPSHOT_ONE);
    }

    @Test
    void shouldSwapReferenceWhenVersionChanges() {
        // given
        given(snapshotSource.getCurrentSanctionsSet()).willReturn(SNAPSHOT_ONE, SNAPSHOT_TWO);
        cache.initialize();

        // when
        cache.refresh();

        // then
        assertThat(cache.getCurrentSanctionsSet()).usingRecursiveComparison().isEqualTo(SNAPSHOT_TWO);
    }

    @Test
    void shouldKeepCurrentReferenceWhenVersionUnchanged() {
        // given
        given(snapshotSource.getCurrentSanctionsSet()).willReturn(SNAPSHOT_ONE);
        cache.initialize();
        var afterInit = cache.getCurrentSanctionsSet();

        // when
        cache.refresh();

        // then
        assertThat(cache.getCurrentSanctionsSet()).isSameAs(afterInit);
    }

    @Test
    void shouldContainNormalizedSanctionedAddress() {
        // given
        given(snapshotSource.getCurrentSanctionsSet()).willReturn(SNAPSHOT_ONE);
        cache.initialize();

        // when / then
        assertThat(cache.contains("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).isTrue();
    }
}
