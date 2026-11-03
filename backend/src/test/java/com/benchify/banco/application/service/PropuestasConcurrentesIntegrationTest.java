package com.benchify.banco.application.service;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.application.port.in.GestionarBancoUseCase;
import com.benchify.banco.domain.Banco;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Propuestas simultáneas de un mismo usuario frente al límite de bancos pendientes, en
 * transacciones reales: sin rollback, así que cada test borra al final lo que ha creado.
 */
class PropuestasConcurrentesIntegrationTest extends AbstractIntegrationTest {

    private static final BancoCommand PROPUESTA = new BancoCommand("Propuesto", null, -47.5, 62.5, null, null,
            null, null, false, false, false, null, null);

    @Autowired
    private GestionarBancoUseCase gestionarBanco;
    @Autowired
    private TestData testData;
    @Autowired
    private JdbcTemplate jdbc;

    private final ExecutorService peticiones = Executors.newFixedThreadPool(2);
    private String usuario;

    @BeforeEach
    void datos() {
        usuario = "conc-prop-" + UUID.randomUUID().toString().substring(0, 8);
        testData.usuario(usuario);
    }

    @AfterEach
    void limpiar() {
        peticiones.shutdownNow();
        jdbc.update("DELETE FROM bancos WHERE creado_por = ?", usuario);
        jdbc.update("DELETE FROM usuarios WHERE id = ?", usuario);
    }

    @Test
    void theSixthPendingProposalIsRejected() {
        for (int i = 0; i < Banco.MAX_PENDIENTES_POR_AUTOR; i++) {
            gestionarBanco.proponer(Actor.user(usuario), PROPUESTA);
        }

        assertThatThrownBy(() -> gestionarBanco.proponer(Actor.user(usuario), PROPUESTA))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("BANCO_LIMITE_PENDIENTES");
        assertThat(pendientes()).isEqualTo(Banco.MAX_PENDIENTES_POR_AUTOR);
    }

    @Test
    void twoSimultaneousProposalsCannotBothTakeTheLastFreeSlot() throws Exception {
        for (int i = 0; i < Banco.MAX_PENDIENTES_POR_AUTOR - 1; i++) {
            gestionarBanco.proponer(Actor.user(usuario), PROPUESTA);
        }
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> peticion = () -> {
            salida.await(10, SECONDS);
            try {
                gestionarBanco.proponer(Actor.user(usuario), PROPUESTA);
                return true;
            } catch (BusinessRuleException e) {
                return false;
            }
        };
        List<Future<Boolean>> resultados = List.of(peticiones.submit(peticion), peticiones.submit(peticion));
        salida.countDown();

        assertThat(List.of(resultados.get(0).get(10, SECONDS), resultados.get(1).get(10, SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(pendientes()).isEqualTo(Banco.MAX_PENDIENTES_POR_AUTOR);
    }

    private long pendientes() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM bancos WHERE creado_por = ? AND estado = 'PENDIENTE'",
                Long.class, usuario);
        return total == null ? 0 : total;
    }
}
