package com.benchify.resena.infrastructure.web;

import com.benchify.resena.application.port.in.ResenasUseCase.ResenaCommand;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.infrastructure.web.dto.ResenaRequest;
import com.benchify.resena.infrastructure.web.dto.ResenaResponse;
import org.mapstruct.Mapper;

@Mapper
interface ResenaWebMapper {

    ResenaResponse toResponse(Resena resena);

    ResenaCommand toCommand(ResenaRequest request);
}
