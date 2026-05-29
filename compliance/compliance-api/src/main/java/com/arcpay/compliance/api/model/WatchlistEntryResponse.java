package com.arcpay.compliance.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatchlistEntryResponse(
        String address,
        String label
) {}
