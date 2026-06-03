package com.arcpay.settlement.application.webhook;

import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import com.arcpay.settlement.domain.TransferNotificationHandler;
import com.arcpay.settlement.domain.port.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/circle")
@RequiredArgsConstructor
@Validated
public class CircleWebhookController {

    private static final String KEY_ID_HEADER = "X-Circle-Key-Id";
    private static final String SIGNATURE_HEADER = "X-Circle-Signature";

    private final WebhookSignatureVerifier signatureVerifier;
    private final CircleNotificationParser notificationParser;
    private final TransferNotificationHandler notificationHandler;

    @RequestMapping(method = HEAD)
    public ResponseEntity<Void> acknowledgeEndpointVerificationProbe() {
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String body,
            @RequestHeader(name = KEY_ID_HEADER, required = false) String keyId,
            @RequestHeader(name = SIGNATURE_HEADER, required = false) String signature) {

        signatureVerifier.verify(body, keyId, signature);
        notificationParser
                .parse(body)
                .ifPresentOrElse(
                        notification -> {
                            log.info(
                                    "Circle webhook accepted circleTxId={} state={}",
                                    notification.circleTxId(),
                                    notification.state());
                            notificationHandler.handle(notification);
                        },
                        () -> log.info("Circle webhook acknowledged (non-transaction notification)"));
        return ResponseEntity.ok().build();
    }
}
