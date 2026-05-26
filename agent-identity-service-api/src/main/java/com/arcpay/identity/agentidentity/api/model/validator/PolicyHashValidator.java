package com.arcpay.identity.agentidentity.api.model.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PolicyHashValidator implements ConstraintValidator<ValidPolicyHash, String> {

    private static final Pattern POLICY_HASH_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{64}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is the caller's job (optional field)
        }
        return POLICY_HASH_PATTERN.matcher(value).matches();
    }
}
