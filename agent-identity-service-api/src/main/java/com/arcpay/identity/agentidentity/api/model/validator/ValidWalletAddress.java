package com.arcpay.identity.agentidentity.api.model.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = WalletAddressValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWalletAddress {
    String message() default "Invalid wallet address: must start with 0x followed by 40 hex characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
