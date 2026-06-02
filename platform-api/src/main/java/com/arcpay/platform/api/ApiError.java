package com.arcpay.platform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String status, String message, Detail details) {

    @Builder
    public record Detail(Map<String, List<String>> errors) {}
}
