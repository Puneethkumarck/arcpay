package com.arcpay.policy.policyengine.application.controller.internal.mapper;

import com.arcpay.policy.policyengine.api.model.ReservationResponse;
import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.model.ReservationStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationResponseMapper {

    ReservationResponse toApi(Reservation reservation);

    default String map(ReservationStatus status) {
        return status.name();
    }
}
