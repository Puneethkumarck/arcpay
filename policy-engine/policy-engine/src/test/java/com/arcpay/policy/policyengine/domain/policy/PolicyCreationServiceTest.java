package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_COMPUTED_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_RULES;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyCreationServiceTest {

    private final PolicyCreationService policyCreationService = new PolicyCreationService();

    @Test
    void shouldCreatePolicyWithUuidV7() {
        // given / when
        var policy = policyCreationService.createPolicy(
                SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 1);

        // then
        assertThat(UuidCreator.getTimeOrderedEpoch().version()).isEqualTo(policy.policyId().version());
        assertThat(policy.policyId().version()).isEqualTo(7);
    }

    @Test
    void shouldSetVersionToProvidedValue() {
        // given / when
        var policy = policyCreationService.createPolicy(
                SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 3);

        // then
        var expected = Policy.builder()
                .policyId(policy.policyId())
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .version(3)
                .rules(SOME_RULES)
                .policyHash(SOME_COMPUTED_HASH)
                .status(PolicyStatus.ACTIVE)
                .createdAt(policy.createdAt())
                .updatedAt(policy.updatedAt())
                .build();
        assertThat(policy).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldCreateActivePolicyForFirstVersion() {
        // given / when
        var policy = policyCreationService.createPolicy(
                SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 1);

        // then
        assertThat(policy.version()).isEqualTo(1);
        assertThat(policy.status()).isEqualTo(PolicyStatus.ACTIVE);
    }
}
