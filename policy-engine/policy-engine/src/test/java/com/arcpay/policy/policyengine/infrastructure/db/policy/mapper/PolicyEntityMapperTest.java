package com.arcpay.policy.policyengine.infrastructure.db.policy.mapper;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.infrastructure.db.policy.PolicyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_RULES;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyEntityMapperTest {

    private final PolicyEntityMapper mapper = Mappers.getMapper(PolicyEntityMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapDomainToEntity() {
        // given
        var policy = SOME_ACTIVE_POLICY;

        // when
        var result = mapper.mapToEntity(policy);

        // then
        var expected = PolicyEntity.builder()
                .policyId(policy.policyId())
                .agentId(policy.agentId())
                .ownerId(policy.ownerId())
                .version(policy.version())
                .rules(mapper.rulesToJson(policy.rules()))
                .policyHash(policy.policyHash())
                .status(policy.status())
                .createdAt(policy.createdAt())
                .updatedAt(policy.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapEntityToDomain() {
        // given
        var policy = SOME_ACTIVE_POLICY;
        var entity = PolicyEntity.builder()
                .policyId(policy.policyId())
                .agentId(policy.agentId())
                .ownerId(policy.ownerId())
                .version(policy.version())
                .rules(mapper.rulesToJson(policy.rules()))
                .policyHash(policy.policyHash())
                .status(policy.status())
                .createdAt(policy.createdAt())
                .updatedAt(policy.updatedAt())
                .build();

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void shouldSerializeRulesToJson() throws JsonProcessingException {
        // given
        var rules = SOME_RULES;

        // when
        var json = mapper.rulesToJson(rules);

        // then
        var deserialized = objectMapper.readValue(json, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, PolicyRule.class));
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(rules);
    }

    @Test
    void shouldDeserializeJsonToRules() {
        // given
        var json = mapper.rulesToJson(SOME_RULES);

        // when
        var result = mapper.jsonToRules(json);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_RULES);
    }
}
