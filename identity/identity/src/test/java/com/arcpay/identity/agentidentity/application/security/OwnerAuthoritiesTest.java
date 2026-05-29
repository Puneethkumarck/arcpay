package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.infrastructure.security.Roles;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerAuthoritiesTest {

    @Test
    void shouldResolveComplianceOfficerForConfiguredKeyHash() {
        // given
        var ownerAuthorities = new OwnerAuthorities(new IdentitySecurityProperties(List.of("officer-hash")));

        // when
        var authority = ownerAuthorities.forApiKeyHash("officer-hash");

        // then
        assertThat(authority).isEqualTo(Roles.COMPLIANCE_OFFICER);
    }

    @Test
    void shouldResolveOwnerForUnconfiguredKeyHash() {
        // given
        var ownerAuthorities = new OwnerAuthorities(new IdentitySecurityProperties(List.of("officer-hash")));

        // when
        var authority = ownerAuthorities.forApiKeyHash("some-other-hash");

        // then
        assertThat(authority).isEqualTo(Roles.OWNER);
    }

    @Test
    void shouldResolveOwnerWhenNoComplianceOfficerKeysConfigured() {
        // given
        var ownerAuthorities = new OwnerAuthorities(new IdentitySecurityProperties(null));

        // when
        var authority = ownerAuthorities.forApiKeyHash("any-hash");

        // then
        assertThat(authority).isEqualTo(Roles.OWNER);
    }
}
