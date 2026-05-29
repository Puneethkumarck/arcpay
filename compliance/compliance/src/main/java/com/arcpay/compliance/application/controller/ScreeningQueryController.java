package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.exception.ScreeningNotFoundException;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ScreeningStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ScreeningQueryController {

    private final ScreeningStore screeningStore;
    private final HoldReviewStore holdReviewStore;
    private final ScreeningQueryMapper screeningQueryMapper;

    @GetMapping("/compliance/screenings/{paymentId}")
    public ScreeningQueryResponse getScreening(@PathVariable UUID paymentId) {
        log.info("Screening query requested paymentId={}", paymentId);
        return screeningStore.findByPaymentId(paymentId)
                .map(screeningQueryMapper::toApi)
                .orElseThrow(() -> new ScreeningNotFoundException(paymentId));
    }

    @GetMapping("/compliance/holds")
    public List<HoldReviewResponse> listHolds(
            @RequestParam(name = "state", defaultValue = "PENDING") ReviewState state) {
        log.info("Hold queue query requested state={}", state);
        return holdReviewStore.findByStateOrderByCreatedAtDesc(state).stream()
                .map(HoldReviewResponse::from)
                .toList();
    }

    @GetMapping("/compliance/holds/{paymentId}")
    public HoldReviewResponse getHold(@PathVariable UUID paymentId) {
        log.info("Hold query requested paymentId={}", paymentId);
        return holdReviewStore.findByPaymentId(paymentId)
                .map(HoldReviewResponse::from)
                .orElseThrow(() -> new HoldNotFoundException(paymentId));
    }
}
