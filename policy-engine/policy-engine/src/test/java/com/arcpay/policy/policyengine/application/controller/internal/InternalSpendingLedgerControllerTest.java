package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.SpendingSummaryResponse;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.SpendingResponseMapper;
import com.arcpay.policy.policyengine.domain.spending.SpendingLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_EXECUTED_AT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_SPENDING_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalSpendingLedgerControllerTest {

    private static final int TWENTY_FOUR_HOURS_IN_MINUTES = 24 * 60;

    @Mock
    private SpendingLedgerService spendingLedgerService;

    private final SpendingResponseMapper spendingResponseMapper =
            Mappers.getMapper(SpendingResponseMapper.class);

    private InternalSpendingLedgerController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalSpendingLedgerController(spendingLedgerService, spendingResponseMapper);
    }

    @Test
    void shouldReturnSpendingSummaryUsingTwentyFourHourWindow() {
        // given
        given(spendingLedgerService.getSpendingSummary(SOME_AGENT_ID, TWENTY_FOUR_HOURS_IN_MINUTES))
                .willReturn(SOME_SPENDING_SUMMARY);

        // when
        var response = controller.getSpendingSummary(SOME_AGENT_ID);

        // then
        var expected = SpendingSummaryResponse.builder()
                .agentId(SOME_AGENT_ID)
                .dailyTotal(SOME_SPENDING_SUMMARY.dailyTotal())
                .weeklyTotal(SOME_SPENDING_SUMMARY.weeklyTotal())
                .monthlyTotal(SOME_SPENDING_SUMMARY.monthlyTotal())
                .transactionCount24h(SOME_SPENDING_SUMMARY.velocityCount())
                .lastTransactionAt(SOME_EXECUTED_AT)
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
