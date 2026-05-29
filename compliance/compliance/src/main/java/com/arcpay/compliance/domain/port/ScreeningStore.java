package com.arcpay.compliance.domain.port;

import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreeningStore {

    void insert(ScreeningResult result, List<ScreeningCheck> checks);

    Optional<ScreeningResult> findByPaymentId(UUID paymentId);
}
