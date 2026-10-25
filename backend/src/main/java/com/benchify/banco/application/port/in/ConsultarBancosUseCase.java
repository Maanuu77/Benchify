package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.List;
import java.util.UUID;

public interface ConsultarBancosUseCase {

    /** Máximo de marcadores que devuelve una consulta del mapa; el cliente agrupa en clusters. */
    int MAX_RESULTADOS_MAPA = 500;
    int RADIO_MAXIMO_METROS = 50_000;

    /** @param actor quien consulta, o {@code null} si es anónimo */
    Banco obtener(UUID bancoId, Actor actor);

    List<Banco> enArea(BoundingBox area, FiltroBancos filtro);

    PageResult<BancoCercano> cercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro, PageQuery page);

    /** Bancos de un usuario: él mismo ve también los pendientes; el resto, solo los publicados. */
    PageResult<Banco> deUsuario(String usuarioId, Actor actor, PageQuery page);

    List<Etiqueta> etiquetas();
}
