package com.arcpay.identity.agentidentity.domain.exception;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    private static final UUID SOME_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

    @Test
    void shouldIncludeAgentIdInAgentNotFoundException() {
        var exception = new AgentNotFoundException(SOME_ID);

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(SOME_ID.toString());
    }

    @Test
    void shouldIncludeContextInAgentNotInExpectedStateException() {
        var exception = new AgentNotInExpectedStateException(
                SOME_ID, AgentStatus.PROVISIONING, AgentStatus.ACTIVE);

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(SOME_ID.toString())
                .hasMessageContaining("PROVISIONING")
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void shouldIncludeNameAndOwnerInAgentNameDuplicateException() {
        var exception = new AgentNameDuplicateException("my-agent", SOME_ID);

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("my-agent")
                .hasMessageContaining(SOME_ID.toString());
    }

    @Test
    void shouldIncludeEmailInOwnerEmailAlreadyExistsException() {
        var exception = new OwnerEmailAlreadyExistsException("alice@example.com");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("alice@example.com");
    }

    @Test
    void shouldIncludeWalletInOwnerWalletAlreadyExistsException() {
        var exception = new OwnerWalletAlreadyExistsException("0xabc123");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("0xabc123");
    }

    @Test
    void shouldIncludeAddressInInvalidWalletAddressException() {
        var exception = new InvalidWalletAddressException("bad-address");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bad-address");
    }

    @Test
    void shouldIncludeEmailInInvalidEmailException() {
        var exception = new InvalidEmailException("not-an-email");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not-an-email");
    }

    @Test
    void shouldIncludeNameInInvalidAgentNameException() {
        var exception = new InvalidAgentNameException("ab");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ab");
    }

    @Test
    void shouldIncludeHashInInvalidPolicyHashException() {
        var exception = new InvalidPolicyHashException("0xbad");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("0xbad");
    }

    @Test
    void shouldHaveMessageInMissingIdempotencyKeyException() {
        var exception = new MissingIdempotencyKeyException();

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    void shouldIncludeResourceAndOwnerInForbiddenException() {
        var exception = new ForbiddenException("agent", SOME_ID);

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("agent")
                .hasMessageContaining(SOME_ID.toString());
    }
}
