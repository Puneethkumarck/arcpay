package com.arcpay.compliance.fixtures;

import com.arcpay.platform.api.OwnerPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;

public final class SecurityContextFixtures {

    public static final String COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String OWNER = "OWNER";

    private SecurityContextFixtures() {}

    public static void authenticate(OwnerPrincipal principal, String authority) {
        SecurityContextHolder.getContext().setAuthentication(tokenFor(principal, authority));
    }

    public static UsernamePasswordAuthenticationToken officerAuth() {
        return tokenFor(new OwnerPrincipal(SOME_OWNER_ID, SOME_OFFICER_EMAIL, COMPLIANCE_OFFICER),
                "ROLE_" + COMPLIANCE_OFFICER);
    }

    public static UsernamePasswordAuthenticationToken ownerAuth() {
        return tokenFor(new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL, OWNER), "ROLE_" + OWNER);
    }

    private static UsernamePasswordAuthenticationToken tokenFor(OwnerPrincipal principal, String authority) {
        var authorities = List.of(new SimpleGrantedAuthority(authority));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
