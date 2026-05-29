package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import com.arcpay.compliance.infrastructure.sanctions.parser.OfacSdnParser;
import com.arcpay.compliance.infrastructure.sanctions.parser.ParserRegistry;
import io.temporal.failure.ApplicationFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.EXPECTED_OFAC_SDN_ADDRESSES;
import static com.arcpay.compliance.fixtures.SanctionsFeedFixtures.OFAC_SDN_FEED;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.SOME_ALL_SOURCES_RECORDS;
import static com.arcpay.compliance.fixtures.SanctionsIngestionFixtures.feedBytes;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SanctionsIngestionActivitiesTest {

    @Mock
    private SanctionsFeedDownloader downloader;

    @Mock
    private ParserRegistry parserRegistry;

    @Mock
    private SanctionsSnapshotWriter snapshotWriter;

    @Mock
    private SanctionsRefreshTracker refreshTracker;

    @InjectMocks
    private SanctionsIngestionActivitiesImpl activities;

    @Test
    void shouldDownloadSourceViaDownloader() {
        // given
        var rawData = feedBytes(OFAC_SDN_FEED);
        given(downloader.download(OFAC_SDN)).willReturn(rawData);

        // when
        var result = activities.downloadSource(OFAC_SDN);

        // then
        assertThat(result).isEqualTo(rawData);
    }

    @Test
    void shouldParseOnlyNormalizedEvmAddresses() {
        // given
        given(parserRegistry.parserFor(OFAC_SDN)).willReturn(new OfacSdnParser());

        // when
        var records = activities.parseAddresses(OFAC_SDN, feedBytes(OFAC_SDN_FEED));

        // then
        var addresses = records.stream().map(SanctionedAddressRecord::address).toList();
        assertThat(addresses).containsExactlyInAnyOrderElementsOf(EXPECTED_OFAC_SDN_ADDRESSES);
    }

    @Test
    void shouldReturnVersionIdWhenSnapshotValid() {
        // when
        var versionId = activities.validateSnapshot(SOME_ALL_SOURCES_RECORDS);

        // then
        assertThat(versionId).isNotNull();
    }

    @Test
    void shouldRejectEmptySnapshot() {
        // given
        Map<SanctionsSource, List<SanctionedAddressRecord>> empty = Map.of();

        // when / then
        assertThatThrownBy(() -> activities.validateSnapshot(empty))
                .isInstanceOf(ApplicationFailure.class);
    }

    @Test
    void shouldPersistSnapshotWithComputedChecksum() {
        // given
        var versionId = activities.validateSnapshot(SOME_ALL_SOURCES_RECORDS);
        var versionCaptor = ArgumentCaptor.forClass(java.util.UUID.class);
        var checksumCaptor = ArgumentCaptor.forClass(String.class);
        var recordsCaptor = ArgumentCaptor.<Map<SanctionsSource, List<SanctionedAddressRecord>>>captor();

        // when
        activities.persistSnapshot(versionId, SOME_ALL_SOURCES_RECORDS);

        // then
        verify(snapshotWriter).persistSnapshot(versionCaptor.capture(), checksumCaptor.capture(),
                recordsCaptor.capture());
        assertThat(versionCaptor.getValue()).isEqualTo(versionId);
        assertThat(checksumCaptor.getValue()).isNotBlank();
        assertThat(recordsCaptor.getValue()).isEqualTo(SOME_ALL_SOURCES_RECORDS);
    }

    @Test
    void shouldRecordSuccessfulRefreshForEverySource() {
        // given
        var versionId = activities.validateSnapshot(SOME_ALL_SOURCES_RECORDS);
        var sourceCaptor = ArgumentCaptor.forClass(SanctionsSource.class);
        var instantCaptor = ArgumentCaptor.forClass(java.time.Instant.class);

        // when
        activities.persistSnapshot(versionId, SOME_ALL_SOURCES_RECORDS);

        // then
        verify(refreshTracker, times(SOME_ALL_SOURCES_RECORDS.size()))
                .recordSuccess(sourceCaptor.capture(), instantCaptor.capture());
        assertThat(sourceCaptor.getAllValues())
                .containsExactlyInAnyOrderElementsOf(SOME_ALL_SOURCES_RECORDS.keySet());
    }

    @Test
    void shouldFlipCurrentVersionViaWriter() {
        // given
        var versionId = activities.validateSnapshot(SOME_ALL_SOURCES_RECORDS);

        // when
        activities.flipCurrentVersion(versionId);

        // then
        verify(snapshotWriter).flipCurrentVersion(versionId);
    }
}
