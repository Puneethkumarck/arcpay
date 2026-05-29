package com.arcpay.compliance.infrastructure.review;

import com.arcpay.compliance.domain.port.OwnerResolver;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OTHER_OWNER_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.authenticate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultReviewAuthorizerTest {

    @Mock
    private OwnerResolver ownerResolver;

    @InjectMocks
    private DefaultReviewAuthorizer reviewAuthorizer;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthorizeOfficerWithoutIdentityCall() {
        // given
        authenticate(new OwnerPrincipal(SOME_OWNER_ID, SOME_OFFICER_EMAIL, Roles.COMPLIANCE_OFFICER),
                "ROLE_" + Roles.COMPLIANCE_OFFICER);

        // when
        var result = reviewAuthorizer.canReview(SOME_OFFICER_EMAIL, SOME_AGENT_ID);

        // then
        assertThat(result).isTrue();
        then(ownerResolver).should(never()).resolveOwner(SOME_AGENT_ID);
    }

    @Test
    void shouldAuthorizeOwnerMatchingResolved() {
        // given
        authenticate(new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL, Roles.OWNER), "ROLE_" + Roles.OWNER);
        given(ownerResolver.resolveOwner(SOME_AGENT_ID)).willReturn(SOME_OWNER_ID);

        // when
        var result = reviewAuthorizer.canReview(SOME_OWNER_EMAIL, SOME_AGENT_ID);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldDenyOwnerMismatch() {
        // given
        authenticate(new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL, Roles.OWNER), "ROLE_" + Roles.OWNER);
        given(ownerResolver.resolveOwner(SOME_AGENT_ID)).willReturn(SOME_OTHER_OWNER_ID);

        // when
        var result = reviewAuthorizer.canReview(SOME_OWNER_EMAIL, SOME_AGENT_ID);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldDenyOfficerAndOwnerPaths() {
        // given
        authenticate(new OwnerPrincipal(SOME_OTHER_OWNER_ID, SOME_OWNER_EMAIL, Roles.OWNER), "ROLE_" + Roles.OWNER);
        given(ownerResolver.resolveOwner(SOME_AGENT_ID)).willReturn(SOME_OWNER_ID);

        // when
        var result = reviewAuthorizer.canReview(SOME_OWNER_EMAIL, SOME_AGENT_ID);

        // then
        assertThat(result).isFalse();
    }
}
