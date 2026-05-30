package com.arcpay.settlement.application.webhook;

import com.arcpay.settlement.domain.model.TransferNotification;
import com.arcpay.settlement.domain.model.TransferState;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_ERROR_REASON;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_NETWORK_FEE;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_TX_HASH;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.failedNotificationBody;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.notificationBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircleNotificationParserTest {

    private final CircleNotificationParser parser =
            new CircleNotificationParser(JsonMapper.builder().build());

    @Test
    void shouldParseCompletedNotification() {
        // given
        var body = notificationBody(SOME_CIRCLE_TX_ID, "COMPLETE");

        // when
        var notification = parser.parse(body);

        // then
        assertThat(notification)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(TransferNotification.builder()
                        .circleTxId(SOME_CIRCLE_TX_ID)
                        .state(TransferState.COMPLETED)
                        .txHash(SOME_TX_HASH)
                        .networkFee(SOME_NETWORK_FEE)
                        .errorReason(null)
                        .build());
    }

    @Test
    void shouldParseFailedNotificationWithErrorReason() {
        // given
        var body = failedNotificationBody(SOME_CIRCLE_TX_ID, "FAILED");

        // when
        var notification = parser.parse(body);

        // then
        assertThat(notification)
                .usingRecursiveComparison()
                .isEqualTo(TransferNotification.builder()
                        .circleTxId(SOME_CIRCLE_TX_ID)
                        .state(TransferState.FAILED)
                        .errorReason(SOME_ERROR_REASON)
                        .build());
    }

    @Test
    void shouldRejectUnknownState() {
        // given
        var body = notificationBody(SOME_CIRCLE_TX_ID, "NONSENSE");

        // when / then
        assertThatThrownBy(() -> parser.parse(body))
                .isInstanceOf(CircleNotificationException.class);
    }

    @Test
    void shouldRejectMalformedJson() {
        // when / then
        assertThatThrownBy(() -> parser.parse("{not-json"))
                .isInstanceOf(CircleNotificationException.class);
    }
}
