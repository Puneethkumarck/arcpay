package com.arcpay.identity.agentidentity.domain.exception;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    private static final UUID SOME_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

    @Test
    void shouldIncludeAgentIdInAgentNotFoundException() {
        // given
        var exception = new AgentNotFoundException(SOME_ID);

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(SOME_ID.toString());
    }

    @Test
    void shouldIncludeContextInAgentNotInExpectedStateException() {
        // given
        var exception = new AgentNotInExpectedStateException(
                SOME_ID, AgentStatus.PROVISIONING, AgentStatus.ACTIVE);

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(SOME_ID.toString())
                .hasMessageContaining("PROVISIONING")
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void shouldIncludeNameAndOwnerInAgentNameDuplicateException() {
        // given
        var exception = new AgentNameDuplicateException("my-agent", SOME_ID);

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("my-agent")
                .hasMessageContaining(SOME_ID.toString());
    }

    @Test
    void shouldIncludeEmailInOwnerEmailAlreadyExistsException() {
        // given
        var exception = new OwnerEmailAlreadyExistsException("alice@example.com");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("alice@example.com");
    }

    @Test
    void shouldIncludeWalletInOwnerWalletAlreadyExistsException() {
        // given
        var exception = new OwnerWalletAlreadyExistsException("0xabc123");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("0xabc123");
    }

    @Test
    void shouldIncludeAddressInInvalidWalletAddressException() {
        // given
        var exception = new InvalidWalletAddressException("bad-address");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bad-address");
    }

    @Test
    void shouldIncludeEmailInInvalidEmailException() {
        // given
        var exception = new InvalidEmailException("not-an-email");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not-an-email");
    }

    @Test
    void shouldIncludeNameInInvalidAgentNameException() {
        // given
        var exception = new InvalidAgentNameException("ab");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ab");
    }

    @Test
    void shouldIncludeHashInInvalidPolicyHashException() {
        // given
        var exception = new InvalidPolicyHashException("0xbad");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("0xbad");
    }

    @Test
    void shouldHaveMessageInMissingIdempotencyKeyException() {
        // given
        var exception = new MissingIdempotencyKeyException();

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    void shouldIncludeResourceAndOwnerInForbiddenException() {
        // given
        var exception = new ForbiddenException("agent", SOME_ID);

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("agent")
                .hasMessageContaining(SOME_ID.toString());
    }
}
