package com.arcpay.compliance.application.controller;

import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
public record ScreeningCheckResponse(
        String type,
        String result,
        int matchScore,
        Map<String, Object> details
) {
}
