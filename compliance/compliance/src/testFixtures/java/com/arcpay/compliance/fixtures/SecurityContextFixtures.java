package com.arcpay.compliance.fixtures;

import com.arcpay.platform.api.OwnerPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class SecurityContextFixtures {

    private SecurityContextFixtures() {}

    public static void authenticate(OwnerPrincipal principal, String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
