package com.arcpay.identity.agentidentity.application.controller.owner.mapper;

import com.arcpay.identity.agentidentity.api.model.OwnerResponse;
import com.arcpay.identity.agentidentity.api.model.OwnerStatusEnum;
import com.arcpay.identity.agentidentity.domain.model.OwnerWithApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OwnerResponseMapper {

    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "owner.email", target = "email")
    @Mapping(source = "owner.walletAddress", target = "walletAddress")
    @Mapping(source = "rawApiKey", target = "apiKey")
    @Mapping(source = "owner.status", target = "status")
    @Mapping(source = "owner.createdAt", target = "createdAt")
    OwnerResponse toApi(OwnerWithApiKey result);

    default OwnerStatusEnum map(com.arcpay.identity.agentidentity.domain.model.OwnerStatus status) {
        return OwnerStatusEnum.valueOf(status.name());
    }
}
