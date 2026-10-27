package com.benchify.shared.infrastructure.web;

import com.benchify.shared.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Parámetros {@code ?page=&size=} comunes a los listados paginados. */
public record PageParams(
        @Schema(description = "Página (base 0)", defaultValue = "0")
        @Min(0) Integer page,
        @Schema(description = "Elementos por página", defaultValue = "20", maximum = "100")
        @Min(1) @Max(PageQuery.MAX_SIZE) Integer size) {

    public PageQuery toQuery() {
        return new PageQuery(page == null ? 0 : page, size == null ? PageQuery.DEFAULT_SIZE : size);
    }
}
