package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.application.dto.ScreeningCheckResponse;
import com.arcpay.compliance.application.dto.ScreeningQueryResponse;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.ScreeningStore;
import com.arcpay.compliance.test.RestControllerAbstractTest;
import com.arcpay.platform.api.ApiError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_OTHER_HOLD_REVIEW_APPROVED;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_OTHER_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_OTHER_SCREENING_RESULT_HOLD;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENED_AT;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_HOLD;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.officerAuth;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.ownerAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreeningQueryControllerIntegrationTest extends RestControllerAbstractTest {

    @Autowired
    private ScreeningStore screeningStore;

    @Autowired
    private HoldReviewStore holdReviewStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void cleanDatabase() {
        wipe();
    }

    @AfterEach
    void tearDown() {
        wipe();
    }

    @Test
    void shouldReturnScreeningMatchingFunctionalSpecSchema() throws Exception {
        // given
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());

        // when
        var response = mockMvc.perform(get("/compliance/screenings/{paymentId}", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HOLD"))
                .andExpect(jsonPath("$.checks[0].result").value("FLAGGED"))
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ScreeningQueryResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(ScreeningQueryResponse.builder()
                .screeningId(SOME_SCREENING_ID)
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_SCREENING_RESULT_HOLD.agentId())
                .recipientAddress(SOME_SCREENING_RESULT_HOLD.recipientAddress())
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
                .build());
    }

    @Test
    void shouldReturnNotFoundWhenScreeningAbsent() throws Exception {
        // given
        var missing = UUID.fromString("0197aa00-9999-7def-8000-999999999999");

        // when
        var response = mockMvc.perform(get("/compliance/screenings/{paymentId}", missing)
                        .with(authentication(officerAuth())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.SCREENING_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnOnlyPendingHoldsSortedByCreatedAtDesc() throws Exception {
        // given
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());
        screeningStore.insert(SOME_OTHER_SCREENING_RESULT_HOLD, SOME_OTHER_SCREENING_RESULT_HOLD.checks());
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        holdReviewStore.insert(SOME_OTHER_HOLD_REVIEW_PENDING);

        // when
        var response = mockMvc.perform(get("/compliance/holds")
                        .with(authentication(officerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, new TypeReference<List<HoldReviewResponse>>() {});
        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(
                HoldReviewResponse.from(SOME_OTHER_HOLD_REVIEW_PENDING),
                HoldReviewResponse.from(SOME_HOLD_REVIEW_PENDING)));
    }

    @Test
    void shouldFilterHoldsByApprovedState() throws Exception {
        // given
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());
        screeningStore.insert(SOME_OTHER_SCREENING_RESULT_HOLD, SOME_OTHER_SCREENING_RESULT_HOLD.checks());
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        holdReviewStore.insert(SOME_OTHER_HOLD_REVIEW_APPROVED);

        // when
        var response = mockMvc.perform(get("/compliance/holds").param("state", "APPROVED")
                        .with(authentication(officerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, new TypeReference<List<HoldReviewResponse>>() {});
        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(
                HoldReviewResponse.from(SOME_OTHER_HOLD_REVIEW_APPROVED)));
    }

    @Test
    void shouldReturnHoldByPaymentId() throws Exception {
        // given
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);

        // when
        var response = mockMvc.perform(get("/compliance/holds/{paymentId}", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, HoldReviewResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(
                HoldReviewResponse.from(SOME_HOLD_REVIEW_PENDING));
    }

    @Test
    void shouldReturnNotFoundWhenHoldAbsent() throws Exception {
        // given
        var missing = UUID.fromString("0197aa00-aaaa-7def-8000-aaaaaaaaaaaa");

        // when
        var response = mockMvc.perform(get("/compliance/holds/{paymentId}", missing)
                        .with(authentication(officerAuth())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.HOLD_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnBadRequestWhenStateIsInvalid() throws Exception {
        // given
        var invalidState = "FOO";

        // when
        var response = mockMvc.perform(get("/compliance/holds").param("state", invalidState)
                        .with(authentication(officerAuth())))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.MALFORMED_REQUEST, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectNonOfficerScreeningQuery() throws Exception {
        // given
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());

        // when
        // then
        mockMvc.perform(get("/compliance/screenings/{paymentId}", SOME_PAYMENT_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectNonOfficerHoldQueueQuery() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/compliance/holds")
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectNonOfficerHoldQuery() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/compliance/holds/{paymentId}", SOME_PAYMENT_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedHoldQueueQuery() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/compliance/holds"))
                .andExpect(status().isUnauthorized());
    }

    private void assertCode(String response, String code, HttpStatus status) {
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual).usingRecursiveComparison().ignoringFields("message", "details").isEqualTo(ApiError.builder()
                .code(code)
                .status(status.getReasonPhrase())
                .build());
    }

    private void wipe() {
        jdbcTemplate.update("DELETE FROM compliance_outbox_record");
        jdbcTemplate.update("DELETE FROM hold_review");
        jdbcTemplate.update("DELETE FROM screening_check");
        jdbcTemplate.update("DELETE FROM screening_result");
    }
}
