package com.arcpay.compliance.infrastructure.review;

import com.arcpay.compliance.domain.port.OwnerResolver;
import com.arcpay.compliance.domain.port.ReviewAuthorizer;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class DefaultReviewAuthorizer implements ReviewAuthorizer {

    private final OwnerResolver ownerResolver;

    @Override
    public boolean canReview(String principal, String role, UUID agentId) {
        if (Roles.COMPLIANCE_OFFICER.equals(role)) {
            return true;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return ownerResolver.resolveOwner(agentId).equals(ownerIdOf(authentication));
    }

    private UUID ownerIdOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OwnerPrincipal owner) {
            return owner.ownerId();
        }
        return null;
    }
}
