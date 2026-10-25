package com.benchify.banco.domain;

import java.util.Objects;

/** Usuario que propuso el banco. {@code nombre} es informativo y puede faltar. */
public record Autor(String id, String nombre) {

    public Autor {
        Objects.requireNonNull(id, "id");
    }

    public static Autor of(String id) {
        return new Autor(id, null);
    }
}
