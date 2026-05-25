package com.arcpay.identity.agentidentity.infrastructure.db.agent.mapper;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.infrastructure.db.agent.AgentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentEntityMapper {

    AgentEntity mapToEntity(Agent agent);

    Agent mapToDomain(AgentEntity entity);
}
