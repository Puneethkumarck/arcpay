package com.arcpay.compliance.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record WatchlistEntryRequest(
        @NotBlank(message = "Address is required") String address,
        @Size(max = 255, message = "Label must not exceed 255 characters") String label
) {}
