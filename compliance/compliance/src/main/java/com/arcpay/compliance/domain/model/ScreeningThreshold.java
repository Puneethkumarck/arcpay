package com.arcpay.compliance.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ScreeningThreshold(int holdThreshold) {
}
