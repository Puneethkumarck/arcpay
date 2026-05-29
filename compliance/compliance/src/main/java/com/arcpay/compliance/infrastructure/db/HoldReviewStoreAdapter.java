package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class HoldReviewStoreAdapter implements HoldReviewStore {

    private final HoldReviewRepository holdReviewRepository;
    private final HoldReviewMapper holdReviewMapper;

    @Override
    @Transactional
    public void insert(HoldReview review) {
        holdReviewRepository.save(holdReviewMapper.mapToEntity(review));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HoldReview> findByPaymentId(UUID paymentId) {
        return holdReviewRepository.findByPaymentId(paymentId).map(holdReviewMapper::mapToDomain);
    }

    @Override
    @Transactional
    public void update(HoldReview review) {
        holdReviewRepository.save(holdReviewMapper.mapToEntity(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldReview> findPending() {
        return holdReviewRepository.findByStateOrderByCreatedAtAsc(ReviewState.PENDING).stream()
                .map(holdReviewMapper::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldReview> findByStateOrderByCreatedAtDesc(ReviewState state) {
        return holdReviewRepository.findByStateOrderByCreatedAtDesc(state).stream()
                .map(holdReviewMapper::mapToDomain)
                .toList();
    }
}
