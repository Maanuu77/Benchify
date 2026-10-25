package com.benchify.banco.domain;

import com.benchify.shared.domain.BusinessRuleException;

/** Filtros opcionales del mapa; {@code null} significa "sin filtrar". */
public record FiltroBancos(Boolean accesible, Boolean conSombra, Boolean conRespaldo, Long etiquetaId,
        Double puntuacionMinima) {

    public static final FiltroBancos NINGUNO = new FiltroBancos(null, null, null, null, null);

    public FiltroBancos {
        if (puntuacionMinima != null && (puntuacionMinima < 0 || puntuacionMinima > 5)) {
            throw new BusinessRuleException("FILTRO_PUNTUACION_INVALIDA", "La puntuación mínima debe estar entre 0 y 5");
        }
    }
}
