package com.arcpay.identity.agentidentity.application.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "arcpay.security")
record IdentitySecurityProperties(List<String> complianceOfficerKeyHashes) {}
