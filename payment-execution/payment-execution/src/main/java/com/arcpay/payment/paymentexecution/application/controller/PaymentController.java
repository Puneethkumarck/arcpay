package com.arcpay.payment.paymentexecution.application.controller;

import com.arcpay.payment.paymentexecution.api.model.CreatePaymentRequest;
import com.arcpay.payment.paymentexecution.api.model.PaymentListResponse;
import com.arcpay.payment.paymentexecution.api.model.PaymentResponse;
import com.arcpay.payment.paymentexecution.application.controller.mapper.PaymentResponseMapper;
import com.arcpay.payment.paymentexecution.application.service.PaymentCreationService;
import com.arcpay.payment.paymentexecution.application.service.PaymentQueryService;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.platform.api.OwnerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentCreationService paymentCreationService;
    private final PaymentQueryService paymentQueryService;
    private final PaymentResponseMapper paymentResponseMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {
        log.info("Payment create requested agentId={} ownerId={}", request.agentId(), principal.ownerId());
        var result = paymentCreationService.create(principal, request);
        var status = result.created() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(paymentResponseMapper.toApi(result.payment()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable UUID paymentId) {
        var payment = paymentQueryService.getPayment(paymentId, principal.ownerId());
        return paymentResponseMapper.toApi(payment);
    }

    @GetMapping
    public PaymentListResponse listPayments(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = paymentQueryService.listPayments(principal.ownerId(), agentId, status, pageable);
        return paymentResponseMapper.toApi(page);
    }
}
