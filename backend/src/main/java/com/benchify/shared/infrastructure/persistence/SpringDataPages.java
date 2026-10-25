package com.benchify.shared.infrastructure.persistence;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Conversión entre la paginación del dominio y la de Spring Data. */
public final class SpringDataPages {

    private SpringDataPages() {
    }

    public static Pageable pageable(PageQuery query, Sort sort) {
        return PageRequest.of(query.page(), query.size(), sort);
    }

    public static Pageable pageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size());
    }

    public static <E, D> PageResult<D> result(Page<E> page, Function<? super E, ? extends D> mapper) {
        return new PageResult<>(page.getContent().stream().<D>map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
