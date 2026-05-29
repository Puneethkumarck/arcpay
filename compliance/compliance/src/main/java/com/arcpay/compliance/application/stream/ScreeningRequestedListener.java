package com.arcpay.compliance.application.stream;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ScreeningRequestedListener {

    private final ScreeningRequestHandler screeningRequestHandler;

    @KafkaListener(topics = PaymentScreeningRequested.TOPIC, groupId = "compliance")
    void onScreeningRequested(PaymentScreeningRequested event) {
        log.info("Received screening request paymentId={} agentId={}", event.paymentId(), event.agentId());
        screeningRequestHandler.handle(event);
    }
}
