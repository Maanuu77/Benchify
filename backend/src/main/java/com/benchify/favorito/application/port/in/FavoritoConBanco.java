package com.benchify.favorito.application.port.in;

import com.benchify.banco.domain.Banco;
import com.benchify.favorito.domain.Favorito;

/** Favorito junto con el banco al que apunta, para listarlos sin otra petición por banco. */
public record FavoritoConBanco(Favorito favorito, Banco banco) {
}
