package com.arcpay.policy.policyengine.api.model;

import com.arcpay.policy.policyengine.api.PolicyRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record CreatePolicyRequest(@NotNull @Size(min = 1) List<PolicyRule> rules) {}
