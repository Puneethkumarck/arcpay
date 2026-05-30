package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.application.dto.ScreeningCheckResponse;
import com.arcpay.compliance.application.dto.ScreeningQueryResponse;
import com.arcpay.compliance.domain.model.Verdict;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENED_AT;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_HOLD;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_PASS;
import static org.assertj.core.api.Assertions.assertThat;

class ScreeningQueryMapperTest {

    private final ScreeningQueryMapper mapper = Mappers.getMapper(ScreeningQueryMapper.class);

    @Test
    void shouldMapPassResultRenamingScreenedAtToTimestamp() {
        // given
        var result = SOME_SCREENING_RESULT_PASS;

        // when
        var actual = mapper.toApi(result);

        // then
        var expected = ScreeningQueryResponse.builder()
                .screeningId(SOME_SCREENING_ID)
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict("PASS")
                .riskScore(0)
                .checks(List.of(ScreeningCheckResponse.builder()
                        .type("SANCTIONS_OFAC")
                        .result("CLEAR")
                        .matchScore(0)
                        .details(Map.of("source", "OFAC_SDN"))
                        .build()))
                .timestamp(SOME_SCREENED_AT)
                .durationMs(42L)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapHoldResultSerializingVerdictAsString() {
        // given
        var result = SOME_SCREENING_RESULT_HOLD;

        // when
        var actual = mapper.toApi(result);

        // then
        var expected = ScreeningQueryResponse.builder()
                .screeningId(SOME_SCREENING_ID)
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict("HOLD")
                .riskScore(100)
                .checks(List.of(ScreeningCheckResponse.builder()
                        .type("WATCHLIST")
                        .result("FLAGGED")
                        .matchScore(100)
                        .details(Map.of("label", "operator-flagged"))
                        .build()))
                .timestamp(SOME_SCREENED_AT)
                .durationMs(58L)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapBlockResultSerializingVerdictAsString() {
        // given
        var result = SOME_SCREENING_RESULT_HOLD.toBuilder().verdict(Verdict.BLOCK).build();

        // when
        var actual = mapper.toApi(result);

        // then
        var expected = ScreeningQueryResponse.builder()
                .screeningId(SOME_SCREENING_ID)
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .verdict("BLOCK")
                .riskScore(100)
                .checks(List.of(ScreeningCheckResponse.builder()
                        .type("WATCHLIST")
                        .result("FLAGGED")
                        .matchScore(100)
                        .details(Map.of("label", "operator-flagged"))
                        .build()))
                .timestamp(SOME_SCREENED_AT)
                .durationMs(58L)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
