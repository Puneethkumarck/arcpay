package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScreeningQueryMapper {

    @Mapping(target = "timestamp", source = "screenedAt")
    ScreeningQueryResponse toApi(ScreeningResult result);

    ScreeningCheckResponse toApi(ScreeningCheck check);
}
