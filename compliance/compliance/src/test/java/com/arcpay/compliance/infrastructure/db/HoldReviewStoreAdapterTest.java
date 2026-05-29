package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.ReviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HoldReviewStoreAdapterTest {

    @Mock
    private HoldReviewRepository holdReviewRepository;

    private final HoldReviewMapper holdReviewMapper = Mappers.getMapper(HoldReviewMapper.class);

    private HoldReviewStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new HoldReviewStoreAdapter(holdReviewRepository, holdReviewMapper);
    }

    @Test
    void shouldInsertHoldReviewAsPending() {
        // given
        var expectedEntity = holdReviewMapper.mapToEntity(SOME_HOLD_REVIEW_PENDING);

        // when
        adapter.insert(SOME_HOLD_REVIEW_PENDING);

        // then
        then(holdReviewRepository).should().save(eqIgnoring(expectedEntity));
    }

    @Test
    void shouldUpdateHoldReviewToApproved() {
        // given
        var approved = SOME_HOLD_REVIEW_PENDING.approve(
                "officer@arcpay.io", "COMPLIANCE_OFFICER", "Verified legitimate counterparty.");
        var expectedEntity = holdReviewMapper.mapToEntity(approved);

        // when
        adapter.update(approved);

        // then
        then(holdReviewRepository).should().save(eqIgnoring(expectedEntity, "decidedAt"));
    }

    @Test
    void shouldFindHoldReviewByPaymentId() {
        // given
        var entity = holdReviewMapper.mapToEntity(SOME_HOLD_REVIEW_PENDING);
        given(holdReviewRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(entity));

        // when
        var result = adapter.findByPaymentId(SOME_PAYMENT_ID);

        // then
        assertThat(result).get().usingRecursiveComparison().isEqualTo(SOME_HOLD_REVIEW_PENDING);
    }

    @Test
    void shouldReturnPendingQueueOrderedByCreatedAt() {
        // given
        var entity = holdReviewMapper.mapToEntity(SOME_HOLD_REVIEW_PENDING);
        given(holdReviewRepository.findByStateOrderByCreatedAtAsc(ReviewState.PENDING))
                .willReturn(List.of(entity));

        // when
        var result = adapter.findPending();

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(List.of(SOME_HOLD_REVIEW_PENDING));
    }

    @Test
    void shouldReturnEmptyWhenHoldNotFoundByPaymentId() {
        // given
        given(holdReviewRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when
        var result = adapter.findByPaymentId(SOME_PAYMENT_ID);

        // then
        assertThat(result).isEmpty();
    }
}
