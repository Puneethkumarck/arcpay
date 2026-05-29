package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.infrastructure.security.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OwnerAuthorities {

    private final Set<String> complianceOfficerKeyHashes;

    OwnerAuthorities(IdentitySecurityProperties properties) {
        this.complianceOfficerKeyHashes = properties.complianceOfficerKeyHashes() == null
                ? Set.of()
                : Set.copyOf(properties.complianceOfficerKeyHashes());
    }

    public String forApiKeyHash(String apiKeyHash) {
        return complianceOfficerKeyHashes.contains(apiKeyHash)
                ? Roles.COMPLIANCE_OFFICER
                : Roles.OWNER;
    }
}
