package com.arcpay.settlement.application.webhook;

import com.arcpay.settlement.domain.model.TransferNotification;
import com.arcpay.settlement.domain.model.TransferState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
class CircleNotificationParser {

    private final JsonMapper jsonMapper;

    TransferNotification parse(String body) {
        try {
            var root = jsonMapper.readTree(body);
            var notification = root.has("notification") ? root.get("notification") : root;
            return TransferNotification.builder()
                    .circleTxId(text(notification, "id"))
                    .state(toState(text(notification, "state")))
                    .txHash(text(notification, "txHash"))
                    .networkFee(decimal(notification, "networkFee"))
                    .errorReason(text(notification, "errorReason"))
                    .build();
        } catch (CircleNotificationException e) {
            throw e;
        } catch (Exception e) {
            throw new CircleNotificationException("Malformed Circle notification payload", e);
        }
    }

    private TransferState toState(String state) {
        if (state == null) {
            throw new CircleNotificationException("Circle notification missing state");
        }
        var normalized = "COMPLETE".equals(state) ? "COMPLETED" : state;
        try {
            return TransferState.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new CircleNotificationException("Unknown Circle transfer state=" + state, e);
        }
    }

    private String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        var value = text(node, field);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }
}
