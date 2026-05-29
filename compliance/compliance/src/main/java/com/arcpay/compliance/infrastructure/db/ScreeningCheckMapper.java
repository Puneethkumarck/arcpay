package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.ScreeningCheck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
interface ScreeningCheckMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "screeningId", source = "screeningId")
    @Mapping(target = "type", source = "check.type")
    @Mapping(target = "result", source = "check.result")
    @Mapping(target = "matchScore", source = "check.matchScore")
    @Mapping(target = "details", source = "check.details")
    ScreeningCheckEntity mapToEntity(ScreeningCheck check, UUID id, UUID screeningId);

    ScreeningCheck mapToDomain(ScreeningCheckEntity entity);
}
