package com.benchify.checkin.infrastructure.web;

import com.benchify.checkin.application.port.in.CheckInConBanco;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.checkin.infrastructure.web.dto.CheckInResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface CheckInWebMapper {

    default CheckInResponse toResponse(CheckInConBanco item) {
        return toResponse(item.checkIn(), item.tituloBanco());
    }

    default CheckInResponse toResponse(CheckIn checkIn) {
        return toResponse(checkIn, null);
    }

    @Mapping(target = "id", source = "checkIn.id")
    @Mapping(target = "bancoId", source = "checkIn.bancoId")
    @Mapping(target = "fecha", source = "checkIn.fecha")
    @Mapping(target = "distanciaMetros", expression = "java(Math.round(checkIn.distanciaMetros()))")
    @Mapping(target = "tituloBanco", source = "tituloBanco")
    CheckInResponse toResponse(CheckIn checkIn, String tituloBanco);
}
