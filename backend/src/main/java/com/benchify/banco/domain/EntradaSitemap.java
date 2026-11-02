package com.benchify.banco.domain;

import java.time.Instant;
import java.util.UUID;

/** Banco publicado tal como aparece en sitemap.xml: su ficha y cuándo cambió por última vez. */
public record EntradaSitemap(UUID bancoId, Instant actualizado) {
}
