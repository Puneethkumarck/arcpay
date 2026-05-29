package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.ApiError;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import com.arcpay.policy.policyengine.domain.exception.PolicyHashMismatchException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.PolicyViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    class NotFoundMapping {

        @Test
        void shouldMapPolicyNotFoundTo404() {
            // given
            var ex = new PolicyNotFoundException(SOME_POLICY_ID);

            // when
            var response = handler.handleNotFound(ex);

            // then
            var expected = ApiError.builder()
                    .code("ARCPAY-POLICY-0001")
                    .status(HttpStatus.NOT_FOUND.getReasonPhrase())
                    .message(ex.getMessage())
                    .build();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void shouldMapAgentNotFoundTo404() {
            // given
            var ex = new AgentNotFoundException(SOME_AGENT_ID);

            // when
            var response = handler.handleNotFound(ex);

            // then
            var expected = ApiError.builder()
                    .code("ARCPAY-POLICY-0005")
                    .status(HttpStatus.NOT_FOUND.getReasonPhrase())
                    .message(ex.getMessage())
                    .build();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void shouldMapInvalidPolicyTo400() {
        // given
        var ex = new InvalidPolicyException("rules must not be empty");

        // when
        var response = handler.handleInvalidPolicy(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0002")
                .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("rules must not be empty")
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapPolicyViolationTo422() {
        // given
        var ex = new PolicyViolationException(SOME_AGENT_ID, "DAILY_LIMIT exceeded");

        // when
        var response = handler.handlePolicyViolation(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0003")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapAgentNotActiveTo422() {
        // given
        var ex = new AgentNotActiveException(SOME_AGENT_ID, "SUSPENDED");

        // when
        var response = handler.handleAgentNotActive(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0004")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapAgentOwnershipTo403() {
        // given
        var ex = new AgentOwnershipException(SOME_AGENT_ID, SOME_OWNER_ID);

        // when
        var response = handler.handleAgentOwnership(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0006")
                .status(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapPolicyHashMismatchTo409() {
        // given
        var ex = new PolicyHashMismatchException(SOME_AGENT_ID, "0xexpected", "0xactual");

        // when
        var response = handler.handlePolicyHashMismatch(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0007")
                .status(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapIdentityUnavailableTo503() {
        // given
        var ex = new IdentityServiceUnavailableException("circuit breaker open");

        // when
        var response = handler.handleIdentityUnavailable(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0008")
                .status(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("circuit breaker open")
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapUnexpectedTo500() {
        // given
        var ex = new IllegalStateException("boom");

        // when
        var response = handler.handleUnexpected(ex);

        // then
        var expected = ApiError.builder()
                .code("ARCPAY-POLICY-0050")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .build();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapUnknownNotFoundSubtypeToPolicyNotFound() {
        // given
        var ex = new PolicyNotFoundException(UUID.randomUUID(), "no active policy");

        // when
        var response = handler.handleNotFound(ex);

        // then
        assertThat(response.getBody().code()).isEqualTo("ARCPAY-POLICY-0001");
    }
}
