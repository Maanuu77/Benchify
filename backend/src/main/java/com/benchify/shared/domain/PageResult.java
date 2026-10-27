package com.benchify.shared.domain;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados junto con el total, independiente de Spring Data. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
