package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.ScreeningResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface ScreeningResultMapper {

    ScreeningResultEntity mapToEntity(ScreeningResult domain);

    @Mapping(target = "checks", ignore = true)
    ScreeningResult mapToDomain(ScreeningResultEntity entity);
}
