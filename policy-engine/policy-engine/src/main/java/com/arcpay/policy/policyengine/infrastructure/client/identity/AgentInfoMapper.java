package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface AgentInfoMapper {

    AgentInfo toDomain(AgentResponse response);
}
