package com.arcpay.policy.policyengine.application.controller.mapper;

import com.arcpay.policy.policyengine.api.model.PolicyListResponse;
import com.arcpay.policy.policyengine.api.model.PolicyResponse;
import com.arcpay.policy.policyengine.domain.model.Policy;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUPERSEDED_POLICY;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyResponseMapperTest {

    private final PolicyResponseMapper mapper = Mappers.getMapper(PolicyResponseMapper.class);

    @Test
    void shouldMapPolicyToApi() {
        // given
        var policy = SOME_ACTIVE_POLICY;

        // when
        var result = mapper.toApi(policy);

        // then
        var expected = PolicyResponse.builder()
                .policyId(policy.policyId())
                .agentId(policy.agentId())
                .version(policy.version())
                .rules(policy.rules())
                .policyHash(policy.policyHash())
                .status("ACTIVE")
                .createdAt(policy.createdAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapSupersededStatusToApi() {
        // given
        var policy = SOME_SUPERSEDED_POLICY;

        // when
        var result = mapper.toApi(policy);

        // then
        var expected = PolicyResponse.builder()
                .policyId(policy.policyId())
                .agentId(policy.agentId())
                .version(policy.version())
                .rules(policy.rules())
                .policyHash(policy.policyHash())
                .status("SUPERSEDED")
                .createdAt(policy.createdAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapPageToApi() {
        // given
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<Policy>(List.of(SOME_ACTIVE_POLICY, SOME_SUPERSEDED_POLICY), pageable, 2);

        // when
        var result = mapper.toApi(page);

        // then
        var expected = PolicyListResponse.builder()
                .content(List.of(mapper.toApi(SOME_ACTIVE_POLICY), mapper.toApi(SOME_SUPERSEDED_POLICY)))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
