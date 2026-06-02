package com.arcpay.settlement.application.receipt;

import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.someReceiptCommand;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.arcpay.settlement.domain.port.ReceiptWriter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ReceiptDispatcherTest {

    @Mock
    private ObjectProvider<ReceiptWriter> receiptWriterProvider;

    @Mock
    private ReceiptWriter receiptWriter;

    @Test
    void shouldDispatchReceiptToWriterAsynchronously() {
        // given
        var command = someReceiptCommand();
        given(receiptWriterProvider.getIfAvailable()).willReturn(receiptWriter);
        var dispatcher = new ReceiptDispatcher(receiptWriterProvider);

        // when
        dispatcher.dispatch(command);

        // then
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> then(receiptWriter).should().writeReceipt(command));
    }

    @Test
    void shouldSkipWriteWhenNoWriterConfigured() {
        // given
        var command = someReceiptCommand();
        given(receiptWriterProvider.getIfAvailable()).willReturn(null);
        var dispatcher = new ReceiptDispatcher(receiptWriterProvider);

        // when
        dispatcher.dispatch(command);

        // then
        then(receiptWriter).shouldHaveNoInteractions();
    }
}
