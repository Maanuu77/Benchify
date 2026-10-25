package com.benchify.shared.infrastructure.web;

import com.benchify.shared.domain.PageResult;

import java.util.List;
import java.util.function.Function;

/** Representación JSON de una página. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PageResponse<T> from(PageResult<D> result, Function<? super D, ? extends T> mapper) {
        PageResult<T> mapped = result.map(mapper);
        return new PageResponse<>(mapped.content(), mapped.page(), mapped.size(),
                mapped.totalElements(), mapped.totalPages());
    }
}
