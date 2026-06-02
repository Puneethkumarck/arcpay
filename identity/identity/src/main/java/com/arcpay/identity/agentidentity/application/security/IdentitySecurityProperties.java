package com.arcpay.identity.agentidentity.application.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arcpay.security")
record IdentitySecurityProperties(List<String> complianceOfficerKeyHashes) {}
