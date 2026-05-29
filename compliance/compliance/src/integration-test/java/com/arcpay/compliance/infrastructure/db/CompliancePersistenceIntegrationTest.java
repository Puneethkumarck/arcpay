package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.exception.ScreeningAlreadyExistsException;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.ScreeningStore;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompliancePersistenceIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private ScreeningStore screeningStore;

    @Autowired
    private HoldReviewStore holdReviewStore;

    @Autowired
    private WatchlistStore watchlistStore;

    @Autowired
    @Qualifier("sanctionsSnapshotAdapter")
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ScreeningResult uniqueScreening() {
        return SOME_SCREENING_RESULT_PASS.toBuilder()
                .screeningId(UuidCreator.getTimeOrderedEpoch())
                .paymentId(UuidCreator.getTimeOrderedEpoch())
                .build();
    }

    @Test
    void shouldRoundTripScreeningResultWithJsonbDetails() {
        // given
        var screening = uniqueScreening();

        // when
        screeningStore.insert(screening, screening.checks());
        var found = screeningStore.findByPaymentId(screening.paymentId());

        // then
        assertThat(found).get().usingRecursiveComparison().isEqualTo(screening);
    }

    @Test
    void shouldFindScreeningByPaymentId() {
        // given
        var screening = uniqueScreening();
        screeningStore.insert(screening, screening.checks());

        // when
        var found = screeningStore.findByPaymentId(screening.paymentId());

        // then
        assertThat(found).get().usingRecursiveComparison().isEqualTo(screening);
    }

    @Test
    void shouldRejectDuplicatePaymentIdWithDomainException() {
        // given
        var screening = uniqueScreening();
        screeningStore.insert(screening, List.of());
        var duplicate = screening.toBuilder()
                .screeningId(UuidCreator.getTimeOrderedEpoch())
                .build();

        // when / then
        assertThatThrownBy(() -> screeningStore.insert(duplicate, List.of()))
                .isInstanceOf(ScreeningAlreadyExistsException.class);
    }

    @Test
    void shouldInsertHoldReviewAsPendingAndTransitionToApproved() {
        // given
        var screening = uniqueScreening();
        screeningStore.insert(screening, List.of());
        var pending = SOME_HOLD_REVIEW_PENDING.toBuilder()
                .reviewId(UuidCreator.getTimeOrderedEpoch())
                .screeningId(screening.screeningId())
                .paymentId(screening.paymentId())
                .build();
        holdReviewStore.insert(pending);

        // when
        var approved = pending.approve("officer@arcpay.io", "COMPLIANCE_OFFICER", "Verified legitimate vendor.");
        holdReviewStore.update(approved);
        var found = holdReviewStore.findByPaymentId(screening.paymentId());

        // then
        assertThat(found).get().extracting(HoldReview::state).isEqualTo(ReviewState.APPROVED);
    }

    @Test
    void shouldFindHoldQueueOrderedByCreatedAtAsc() {
        // given
        var earliest = persistPendingReview(Instant.parse("2026-06-01T09:00:00Z"));
        var middle = persistPendingReview(Instant.parse("2026-06-01T10:00:00Z"));
        var latest = persistPendingReview(Instant.parse("2026-06-01T11:00:00Z"));

        // when
        var queue = holdReviewStore.findPending().stream()
                .map(HoldReview::paymentId)
                .filter(List.of(earliest, middle, latest)::contains)
                .toList();

        // then
        assertThat(queue).containsExactly(earliest, middle, latest);
    }

    @Test
    void shouldIdempotentlyAddWatchlistAddressAndNormalizeLowercase() {
        // given
        var address = "0xWATCH" + Long.toHexString(System.nanoTime()) + "0000000000000000";

        // when
        watchlistStore.addAddress(address, "flagged", "officer@arcpay.io");
        watchlistStore.addAddress(address, "flagged-again", "officer@arcpay.io");

        // then
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM watchlist_address WHERE address = ?",
                Integer.class, address.toLowerCase());
        assertThat(count).isEqualTo(1);
        assertThat(watchlistStore.contains(address.toUpperCase())).isTrue();
    }

    @Test
    void shouldLoadCurrentSanctionsSnapshotFromPointer() {
        // given
        var versionId = UuidCreator.getTimeOrderedEpoch();
        var sanctionedAddress = "0xsanctioned" + Long.toHexString(System.nanoTime());
        seedSanctionsVersion(versionId, sanctionedAddress);

        // when
        var snapshot = sanctionsSetProvider.getCurrentSanctionsSet();

        // then
        assertThat(snapshot.versionId()).isEqualTo(versionId);
        assertThat(snapshot.contains(sanctionedAddress)).isTrue();
    }

    private UUID persistPendingReview(Instant createdAt) {
        var screening = uniqueScreening();
        screeningStore.insert(screening, List.of());
        var pending = SOME_HOLD_REVIEW_PENDING.toBuilder()
                .reviewId(UuidCreator.getTimeOrderedEpoch())
                .screeningId(screening.screeningId())
                .paymentId(screening.paymentId())
                .createdAt(createdAt)
                .build();
        holdReviewStore.insert(pending);
        return screening.paymentId();
    }

    private void seedSanctionsVersion(UUID versionId, String address) {
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, address, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
    }
}
