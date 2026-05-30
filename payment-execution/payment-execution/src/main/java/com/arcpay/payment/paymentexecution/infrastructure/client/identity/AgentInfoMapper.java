package com.arcpay.payment.paymentexecution.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentInfoMapper {

    AgentInfo toDomain(AgentResponse response);

    default String map(AgentStatusEnum status) {
        return status == null ? null : status.name();
    }
}
