package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.model.HoldReview;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface HoldReviewMapper {

    HoldReviewEntity mapToEntity(HoldReview domain);

    HoldReview mapToDomain(HoldReviewEntity entity);
}
