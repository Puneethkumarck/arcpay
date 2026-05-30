package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.arcpay.settlement.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someTransactionWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementQueryServiceIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SettlementQueryService queryService;

    @Autowired
    private SettlementTransactionRepository repository;

    @Test
    void shouldReturnPersistedTransferStateForReconciliation() {
        // given
        var paymentId = UUID.randomUUID();
        var transaction = someTransactionWith(paymentId, TransferState.COMPLETED);
        repository.save(transaction);

        // when
        var loaded = queryService.findTransfer(paymentId);

        // then
        assertThat(loaded)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(transaction);
    }

    @Test
    void shouldThrowWhenTransferNotFound() {
        // given
        var randomId = UUID.randomUUID();

        // when / then
        assertThatThrownBy(() -> queryService.findTransfer(randomId))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }
}
