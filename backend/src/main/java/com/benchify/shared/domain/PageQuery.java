package com.benchify.shared.domain;

/** Página solicitada (base 0) con un tamaño acotado para proteger la base de datos. */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
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
