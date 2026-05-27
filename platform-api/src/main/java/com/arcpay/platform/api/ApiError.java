package com.arcpay.platform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String status,
        String message,
        Detail details
) {

    @Builder
    public record Detail(
            Map<String, List<String>> errors
    ) {}
}
