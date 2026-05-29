package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.application.dto.ReviewDecisionRequest;
import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.port.HoldReviewStore;
import com.arcpay.compliance.domain.port.OwnerResolver;
import com.arcpay.compliance.domain.port.ScreeningStore;
import com.arcpay.compliance.test.RestControllerAbstractTest;
import com.arcpay.platform.api.ApiError;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_DECISION_REASON;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_HOLD_REVIEW_PENDING;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_HOLD;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OTHER_OWNER_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.officerAuth;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.ownerAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HoldReviewControllerIntegrationTest extends RestControllerAbstractTest {

    @MockitoBean
    private OwnerResolver ownerResolver;

    @Autowired
    private HoldReviewStore holdReviewStore;

    @Autowired
    private ScreeningStore screeningStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @BeforeEach
    void cleanDatabase() {
        wipe();
        screeningStore.insert(SOME_SCREENING_RESULT_HOLD, SOME_SCREENING_RESULT_HOLD.checks());
    }

    @AfterEach
    void tearDown() {
        wipe();
    }

    @Test
    void shouldApproveHoldViaPostAndReturn200() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, HoldReviewResponse.class);
        assertThat(actual).usingRecursiveComparison().ignoringFields("decidedAt").isEqualTo(
                HoldReviewResponse.from(SOME_HOLD_REVIEW_PENDING).toBuilder()
                        .state(ReviewState.APPROVED)
                        .reviewerPrincipal(SOME_OFFICER_EMAIL)
                        .reviewerRole("COMPLIANCE_OFFICER")
                        .reason(SOME_DECISION_REASON)
                        .build());
        assertThat(holdReviewState()).isEqualTo(ReviewState.APPROVED.name());
        assertThat(awaitEvent(ScreeningApproved.TOPIC, ScreeningApproved.class).reviewer())
                .isEqualTo(SOME_OFFICER_EMAIL);
        then(ownerResolver).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectHoldViaPostAndReturn200() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/reject", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, HoldReviewResponse.class);
        assertThat(actual).usingRecursiveComparison().ignoringFields("decidedAt").isEqualTo(
                HoldReviewResponse.from(SOME_HOLD_REVIEW_PENDING).toBuilder()
                        .state(ReviewState.REJECTED)
                        .reviewerPrincipal(SOME_OFFICER_EMAIL)
                        .reviewerRole("COMPLIANCE_OFFICER")
                        .reason(SOME_DECISION_REASON)
                        .build());
        assertThat(holdReviewState()).isEqualTo(ReviewState.REJECTED.name());
        assertThat(awaitEvent(ScreeningRejected.TOPIC, ScreeningRejected.class).reviewer())
                .isEqualTo(SOME_OFFICER_EMAIL);
    }

    @Test
    void shouldReturnBadRequestForBlankReason() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        var body = "{\"reason\":\"\"}";

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        assertReasonInvalid(response);
    }

    @Test
    void shouldReturnBadRequestForShortReason() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        var body = "{\"reason\":\"short\"}";

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        assertReasonInvalid(response);
    }

    @Test
    void shouldReturnConflictIfHoldAlreadyDecided() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING.toBuilder()
                .state(ReviewState.APPROVED)
                .reviewerPrincipal(SOME_OFFICER_EMAIL)
                .reviewerRole("COMPLIANCE_OFFICER")
                .reason(SOME_DECISION_REASON)
                .build());
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.HOLD_ALREADY_DECIDED, HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnNotFoundIfHoldAbsent() throws Exception {
        // given
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.HOLD_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnForbiddenIfPrincipalNotAuthorized() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        given(ownerResolver.resolveOwner(SOME_AGENT_ID)).willReturn(SOME_OTHER_OWNER_ID);
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        var response = mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        assertCode(response, ErrorCodes.NOT_AUTHORIZED, HttpStatus.FORBIDDEN);
        assertThat(holdReviewState()).isEqualTo(ReviewState.PENDING.name());
    }

    @Test
    void shouldAuthorizeOwnerWhenOwnerResolverMatchesPrincipal() throws Exception {
        // given
        holdReviewStore.insert(SOME_HOLD_REVIEW_PENDING);
        given(ownerResolver.resolveOwner(SOME_AGENT_ID)).willReturn(SOME_OWNER_ID);
        var body = jsonMapper.writeValueAsString(new ReviewDecisionRequest(SOME_DECISION_REASON));

        // when
        mockMvc.perform(post("/compliance/holds/{paymentId}/approve", SOME_PAYMENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // then
        assertThat(holdReviewState()).isEqualTo(ReviewState.APPROVED.name());
        then(ownerResolver).should().resolveOwner(SOME_AGENT_ID);
    }

    private void assertReasonInvalid(String response) {
        assertCode(response, ErrorCodes.REVIEW_REASON_INVALID, HttpStatus.BAD_REQUEST);
    }

    private void assertCode(String response, String code, HttpStatus status) {
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual).usingRecursiveComparison().ignoringFields("message", "details").isEqualTo(ApiError.builder()
                .code(code)
                .status(status.getReasonPhrase())
                .build());
    }

    private String holdReviewState() {
        var states = jdbcTemplate.queryForList(
                "SELECT state FROM hold_review WHERE payment_id = ?", String.class, SOME_PAYMENT_ID);
        return states.isEmpty() ? null : states.getFirst();
    }

    private <T> T awaitEvent(String topic, Class<T> eventType) {
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of(topic));
            var captured = new AtomicReference<T>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            if (SOME_PAYMENT_ID.toString().equals(record.key())) {
                                captured.set(jsonMapper.readValue(record.value(), eventType));
                                return true;
                            }
                        }
                        return false;
                    });
            return captured.get();
        }
    }

    private KafkaConsumer<String, String> newConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hold-review-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private void wipe() {
        jdbcTemplate.update("DELETE FROM compliance_outbox_record");
        jdbcTemplate.update("DELETE FROM hold_review");
        jdbcTemplate.update("DELETE FROM screening_check");
        jdbcTemplate.update("DELETE FROM screening_result");
    }
}
