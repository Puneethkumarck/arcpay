package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.exception.AgentNameDuplicateException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidAgentNameException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPolicyHashException;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AgentValidatorTest {

    private static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    private static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    private static final String VALID_NAME = "my-agent";
    private static final String VALID_PURPOSE = "Trading agent for USDC";
    private static final String VALID_POLICY_HASH = "0x" + "a".repeat(64);

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentValidator agentValidator;

    @Test
    void shouldAcceptValidRegistration() {
        // given / when / then
        assertThatNoException().isThrownBy(
                () -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, VALID_PURPOSE, null));
    }

    @Test
    void shouldAcceptValidRegistrationWithPolicyHash() {
        // given / when / then
        assertThatNoException().isThrownBy(
                () -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, VALID_PURPOSE, VALID_POLICY_HASH));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankName(String name) {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, name, VALID_PURPOSE, null))
                .isInstanceOf(InvalidAgentNameException.class);
    }

    @Test
    void shouldRejectNameTooShort() {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, "ab", VALID_PURPOSE, null))
                .isInstanceOf(InvalidAgentNameException.class);
    }

    @Test
    void shouldRejectNameTooLong() {
        // given
        var longName = "a".repeat(65);

        // when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, longName, VALID_PURPOSE, null))
                .isInstanceOf(InvalidAgentNameException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name with spaces", "name@invalid", "name.dot", "name/slash"})
    void shouldRejectNameWithInvalidChars(String name) {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, name, VALID_PURPOSE, null))
                .isInstanceOf(InvalidAgentNameException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-agent", "my_agent", "Agent123", "a-b-c"})
    void shouldAcceptValidName(String name) {
        // given / when / then
        assertThatNoException().isThrownBy(
                () -> agentValidator.validateRegistration(SOME_OWNER_ID, name, VALID_PURPOSE, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankPurpose(String purpose) {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, purpose, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectPurposeTooLong() {
        // given
        var longPurpose = "a".repeat(257);

        // when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, longPurpose, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptNullPolicyHash() {
        // given / when / then
        assertThatNoException().isThrownBy(
                () -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, VALID_PURPOSE, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0xshort", "not-hex", "0x" + "zz", "abc"})
    void shouldRejectInvalidPolicyHash(String hash) {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, VALID_PURPOSE, hash))
                .isInstanceOf(InvalidPolicyHashException.class);
    }

    @Test
    void shouldRejectDuplicateNameForSameOwner() {
        // given
        given(agentRepository.existsByOwnerIdAndNameIgnoreCase(SOME_OWNER_ID, VALID_NAME)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> agentValidator.validateRegistration(SOME_OWNER_ID, VALID_NAME, VALID_PURPOSE, null))
                .isInstanceOf(AgentNameDuplicateException.class);
    }

    @Test
    void shouldRejectDuplicateNameOnUpdateExcludingSelf() {
        // given
        given(agentRepository.existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(SOME_OWNER_ID, VALID_NAME, SOME_AGENT_ID))
                .willReturn(true);

        // when / then
        assertThatThrownBy(() -> agentValidator.validateUpdate(SOME_OWNER_ID, SOME_AGENT_ID, VALID_NAME, VALID_PURPOSE))
                .isInstanceOf(AgentNameDuplicateException.class);
    }

    @Test
    void shouldAcceptNullFieldsOnUpdate() {
        // given / when / then
        assertThatNoException().isThrownBy(
                () -> agentValidator.validateUpdate(SOME_OWNER_ID, SOME_AGENT_ID, null, null));
    }

    @Test
    void shouldValidatePolicyUpdateWithValidHash() {
        // given / when / then
        assertThatNoException().isThrownBy(() -> agentValidator.validatePolicyUpdate(VALID_POLICY_HASH));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankPolicyUpdate(String hash) {
        // given / when / then
        assertThatThrownBy(() -> agentValidator.validatePolicyUpdate(hash))
                .isInstanceOf(InvalidPolicyHashException.class);
    }
}
