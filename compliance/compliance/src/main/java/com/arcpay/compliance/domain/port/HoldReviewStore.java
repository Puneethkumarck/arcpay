package com.arcpay.compliance.domain.port;

import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldReviewStore {

    void insert(HoldReview review);

    Optional<HoldReview> findByPaymentId(UUID paymentId);

    void update(HoldReview review);

    List<HoldReview> findPending();

    List<HoldReview> findByStateOrderByCreatedAtDesc(ReviewState state);
}
