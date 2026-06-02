package com.arcpay.compliance.domain.model;

import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record ScreeningCheck(CheckType type, CheckResult result, int matchScore, Map<String, Object> details) {

    public ScreeningCheck {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(result, "result must not be null");
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
