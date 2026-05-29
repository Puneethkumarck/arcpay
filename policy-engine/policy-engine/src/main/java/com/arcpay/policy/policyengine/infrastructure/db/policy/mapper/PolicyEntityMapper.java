package com.arcpay.policy.policyengine.infrastructure.db.policy.mapper;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.infrastructure.db.policy.PolicyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PolicyEntityMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    TypeReference<List<PolicyRule>> RULES_TYPE = new TypeReference<>() {};

    @Mapping(target = "rules", source = "rules", qualifiedByName = "rulesToJson")
    PolicyEntity mapToEntity(Policy domain);

    @Mapping(target = "rules", source = "rules", qualifiedByName = "jsonToRules")
    Policy mapToDomain(PolicyEntity entity);

    @Named("rulesToJson")
    default String rulesToJson(List<PolicyRule> rules) {
        try {
            return OBJECT_MAPPER.writerFor(RULES_TYPE).writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize policy rules to JSON", e);
        }
    }

    @Named("jsonToRules")
    default List<PolicyRule> jsonToRules(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, RULES_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize policy rules from JSON", e);
        }
    }
}
