package com.arcpay.compliance.application.dto;

import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record ScreeningCheckResponse(String type, String result, int matchScore, Map<String, Object> details) {}
