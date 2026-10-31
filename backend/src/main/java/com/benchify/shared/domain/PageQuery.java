package com.benchify.shared.domain;

/**
 * Página solicitada (base 0) con un tamaño acotado para proteger la base de datos. La página
 * también tiene un máximo: con OFFSET, pedir una muy alta obliga a recorrer todas las anteriores.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final int MAX_PAGE = 10_000;

    public PageQuery {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("page must be between 0 and " + MAX_PAGE);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageQuery first(int size) {
        return new PageQuery(0, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
