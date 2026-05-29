package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.ReviewState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface HoldReviewRepository extends JpaRepository<HoldReviewEntity, UUID> {

    Optional<HoldReviewEntity> findByPaymentId(UUID paymentId);

    List<HoldReviewEntity> findByStateOrderByCreatedAtAsc(ReviewState state);

    List<HoldReviewEntity> findByStateOrderByCreatedAtDesc(ReviewState state);
}
