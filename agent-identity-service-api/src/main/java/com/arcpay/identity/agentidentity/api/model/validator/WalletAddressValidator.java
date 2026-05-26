package com.arcpay.identity.agentidentity.api.model.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class WalletAddressValidator implements ConstraintValidator<ValidWalletAddress, String> {

    private static final Pattern WALLET_ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is @NotBlank's job
        }
        return WALLET_ADDRESS_PATTERN.matcher(value).matches();
    }
}
