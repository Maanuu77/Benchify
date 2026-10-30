package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PreconditionFailedException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BancoTest {

    static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    static final Actor AUTOR = Actor.user("autor");
    static final Actor OTRO = Actor.user("otro");
    static final Actor ADMIN = Actor.admin("admin");

    static DatosBanco datos(String titulo) {
        return new DatosBanco(titulo, "Junto al río", new Ubicacion(37.3826, -6.0034),
                new DireccionPostal("Calle Betis", "Sevilla", "Sevilla", null),
                new Caracteristicas(true, true, false, MaterialBanco.MADERA), null);
    }

    static Banco pendiente() {
        return Banco.proponer(UUID.randomUUID(), datos("Banco del Betis"), AUTOR, AHORA);
    }

    @Test
    void userProposalsWaitForModeration() {
        Banco banco = pendiente();

        assertThat(banco.estado()).isEqualTo(EstadoBanco.PENDIENTE);
        assertThat(banco.autor().id()).isEqualTo("autor");
        assertThat(banco.valoracion()).isEqualTo(Valoracion.SIN_RESENAS);
        assertThat(banco.fechaVerificacion()).isNull();
    }

    @Test
    void adminProposalsArePublishedDirectly() {
        Banco banco = Banco.proponer(UUID.randomUUID(), datos("Oficial"), ADMIN, AHORA);

        assertThat(banco.estaPublicado()).isTrue();
        assertThat(banco.fechaVerificacion()).isEqualTo(AHORA);
    }

    @Test
    void aUserCannotHaveMoreThanFivePendingProposals() {
        Banco.exigirCupoDePropuestas(AUTOR, () -> Banco.MAX_PENDIENTES_POR_AUTOR - 1);

        assertThatThrownBy(() -> Banco.exigirCupoDePropuestas(AUTOR, () -> Banco.MAX_PENDIENTES_POR_AUTOR))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("BANCO_LIMITE_PENDIENTES");
    }

    @Test
    void adminsHaveNoProposalLimitAndAreNotEvenCounted() {
        Banco.exigirCupoDePropuestas(ADMIN, () -> {
            throw new AssertionError("No debería contar los pendientes de un administrador");
        });
    }

    @Nested
    class Visibilidad {

        @Test
        void publishedBenchesAreVisibleToEveryone() {
            Banco banco = pendiente();
            banco.aprobar(AHORA);

            assertThat(banco.esVisiblePara(null)).isTrue();
            assertThat(banco.esVisiblePara(OTRO)).isTrue();
        }

        @Test
        void pendingBenchesAreVisibleOnlyToAuthorAndAdmins() {
            Banco banco = pendiente();

            assertThat(banco.esVisiblePara(null)).isFalse();
            assertThat(banco.esVisiblePara(OTRO)).isFalse();
            assertThat(banco.esVisiblePara(AUTOR)).isTrue();
            assertThat(banco.esVisiblePara(ADMIN)).isTrue();
        }

        @Test
        void deletedBenchesAreVisibleOnlyToAdmins() {
            Banco banco = pendiente();
            banco.eliminar(AUTOR);

            assertThat(banco.esVisiblePara(AUTOR)).isFalse();
            assertThat(banco.esVisiblePara(ADMIN)).isTrue();
        }
    }

    @Nested
    class Edicion {

        @Test
        void authorEditOfPublishedBenchSendsItBackToModeration() {
            Banco banco = pendiente();
            banco.aprobar(AHORA);

            banco.editar(datos("Nuevo título"), AUTOR);

            assertThat(banco.datos().titulo()).isEqualTo("Nuevo título");
            assertThat(banco.estado()).isEqualTo(EstadoBanco.PENDIENTE);
            assertThat(banco.fechaVerificacion()).isNull();
        }

        @Test
        void adminEditKeepsThePublishedState() {
            Banco banco = pendiente();
            banco.aprobar(AHORA);

            banco.editar(datos("Corregido"), ADMIN);

            assertThat(banco.estaPublicado()).isTrue();
        }

        @Test
        void othersCannotEditOrDelete() {
            Banco banco = pendiente();

            assertThatThrownBy(() -> banco.editar(datos("Hack"), OTRO))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("code").isEqualTo("BANCO_NO_AUTOR");
            assertThatThrownBy(() -> banco.eliminar(OTRO)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        void deletedBenchCannotBeEditedOrDeletedAgain() {
            Banco banco = pendiente();
            banco.eliminar(AUTOR);

            assertThat(banco.estado()).isEqualTo(EstadoBanco.ELIMINADO);
            assertThatThrownBy(() -> banco.editar(datos("x"), ADMIN)).isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> banco.eliminar(ADMIN)).isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void anEditMustStartFromTheCurrentVersion() {
            Banco banco = new Banco(UUID.randomUUID(), datos("Versionado"), EstadoBanco.PUBLICADO, Autor.of("autor"),
                    Valoracion.SIN_RESENAS, AHORA, AHORA, 4);

            banco.exigirVersion(4, AUTOR);
            banco.exigirVersion(4, ADMIN);
            assertThatThrownBy(() -> banco.exigirVersion(3, AUTOR))
                    .isInstanceOf(PreconditionFailedException.class)
                    .extracting("code").isEqualTo("BANCO_VERSION_OBSOLETA");
        }

        @Test
        void permissionsAreCheckedBeforeTheVersion() {
            Banco banco = pendiente();

            assertThatThrownBy(() -> banco.exigirVersion(7, OTRO)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Moderacion {

        @Test
        void approveAndRejectMoveBetweenStates() {
            Banco banco = pendiente();

            banco.aprobar(AHORA);
            assertThat(banco.estaPublicado()).isTrue();
            assertThat(banco.fechaVerificacion()).isEqualTo(AHORA);

            banco.rechazar();
            assertThat(banco.estado()).isEqualTo(EstadoBanco.OCULTO);
            assertThat(banco.fechaVerificacion()).isNull();
        }

        @Test
        void cannotApproveTwiceOrRejectTwice() {
            Banco banco = pendiente();
            banco.aprobar(AHORA);
            assertThatThrownBy(() -> banco.aprobar(AHORA))
                    .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_YA_PUBLICADO");

            banco.rechazar();
            assertThatThrownBy(banco::rechazar)
                    .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_YA_OCULTO");
        }

        @Test
        void deletedBenchCannotBeModerated() {
            Banco banco = pendiente();
            banco.eliminar(AUTOR);

            assertThatThrownBy(() -> banco.aprobar(AHORA)).isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(banco::rechazar).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void ratingIsRoundedToOneDecimal() {
        Banco banco = pendiente();

        banco.actualizarValoracion(new Valoracion(4.26, 3));

        assertThat(banco.valoracion().media()).isEqualTo(4.3);
        assertThat(banco.valoracion().numResenas()).isEqualTo(3);
    }
}
