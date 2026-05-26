package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.exception.AgentNameDuplicateException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidAgentNameException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPolicyHashException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPurposeException;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AgentValidator {

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_PURPOSE_LENGTH = 256;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern POLICY_HASH_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{64}$");

    private final AgentRepository agentRepository;

    public void validateRegistration(UUID ownerId, String name, String purpose, String policyHash) {
        validateName(name);
        validatePurpose(purpose);
        validatePolicyHash(policyHash);
        validateNameUniqueness(ownerId, name);
    }

    public void validateUpdate(UUID ownerId, UUID agentId, String name, String purpose) {
        if (name != null) {
            validateName(name);
            validateNameUniquenessExcluding(ownerId, name, agentId);
        }
        if (purpose != null) {
            validatePurpose(purpose);
        }
    }

    public void validatePolicyUpdate(String policyHash) {
        if (policyHash == null || policyHash.isBlank()) {
            throw new InvalidPolicyHashException(policyHash);
        }
        validatePolicyHash(policyHash);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidAgentNameException(name);
        }
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidAgentNameException(name);
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidAgentNameException(name);
        }
    }

    private void validatePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new InvalidPurposeException("Purpose must not be empty");
        }
        if (purpose.length() > MAX_PURPOSE_LENGTH) {
            throw new InvalidPurposeException("Purpose must not exceed " + MAX_PURPOSE_LENGTH + " characters");
        }
    }

    private void validatePolicyHash(String policyHash) {
        if (policyHash != null && !POLICY_HASH_PATTERN.matcher(policyHash).matches()) {
            throw new InvalidPolicyHashException(policyHash);
        }
    }

    private void validateNameUniqueness(UUID ownerId, String name) {
        if (agentRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name)) {
            throw new AgentNameDuplicateException(name, ownerId);
        }
    }

    private void validateNameUniquenessExcluding(UUID ownerId, String name, UUID agentId) {
        if (agentRepository.existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(ownerId, name, agentId)) {
            throw new AgentNameDuplicateException(name, ownerId);
        }
    }
}
