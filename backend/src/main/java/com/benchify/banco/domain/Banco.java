package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PreconditionFailedException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Agregado raíz de la feature. Las reglas de visibilidad, edición y moderación viven aquí
 * para que ningún adaptador pueda saltárselas.
 */
public class Banco {

    /** Bancos que un usuario puede tener a la vez esperando moderación. */
    public static final int MAX_PENDIENTES_POR_AUTOR = 5;

    private final UUID id;
    private final Autor autor;
    private final Instant fechaSubida;
    private final long version;
    private DatosBanco datos;
    private EstadoBanco estado;
    private Valoracion valoracion;
    private Instant fechaVerificacion;

    /** Reconstruye un banco ya existente (lo usa el adaptador de persistencia). */
    public Banco(UUID id, DatosBanco datos, EstadoBanco estado, Autor autor, Valoracion valoracion,
            Instant fechaSubida, Instant fechaVerificacion, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.datos = Objects.requireNonNull(datos, "datos");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.autor = Objects.requireNonNull(autor, "autor");
        this.valoracion = Objects.requireNonNull(valoracion, "valoracion");
        this.fechaSubida = Objects.requireNonNull(fechaSubida, "fechaSubida");
        this.fechaVerificacion = fechaVerificacion;
        this.version = version;
    }

    /**
     * Un usuario propone un banco nuevo: queda pendiente de moderación.
     * Los administradores publican directamente.
     */
    public static Banco proponer(UUID id, DatosBanco datos, Actor actor, Instant ahora) {
        EstadoBanco estado = actor.isAdmin() ? EstadoBanco.PUBLICADO : EstadoBanco.PENDIENTE;
        return new Banco(id, datos, estado, Autor.of(actor.userId()), Valoracion.SIN_RESENAS, ahora,
                actor.isAdmin() ? ahora : null, 0);
    }

    /**
     * Nadie puede tener más de {@link #MAX_PENDIENTES_POR_AUTOR} propuestas pendientes a la vez, para
     * que un usuario no pueda llenar la cola de moderación. Los administradores publican
     * directamente: no tienen límite y sus pendientes ni se cuentan.
     */
    public static void exigirCupoDePropuestas(Actor actor, LongSupplier pendientesDelAutor) {
        if (!actor.isAdmin() && pendientesDelAutor.getAsLong() >= MAX_PENDIENTES_POR_AUTOR) {
            throw new BusinessRuleException("BANCO_LIMITE_PENDIENTES", "Ya tienes " + MAX_PENDIENTES_POR_AUTOR
                    + " bancos pendientes de moderación; espera a que se revisen antes de proponer otro");
        }
    }

    /** Un banco publicado lo ve todo el mundo; el resto solo su autor y los administradores. */
    public boolean esVisiblePara(Actor actor) {
        if (estado == EstadoBanco.PUBLICADO) {
            return true;
        }
        if (actor == null || estado == EstadoBanco.ELIMINADO && !actor.isAdmin()) {
            return false;
        }
        return actor.canManage(autor.id());
    }

    /**
     * Si el autor edita un banco publicado, los cambios no se aplican aquí: se guardan como una
     * {@link RevisionBanco} y el banco sigue en el mapa con sus datos hasta que se aprueben. Un banco
     * aún no publicado se puede corregir directamente, y un administrador siempre edita directamente.
     */
    public boolean edicionRequiereRevision(Actor actor) {
        return !actor.isAdmin() && estado == EstadoBanco.PUBLICADO;
    }

    /** El autor y los administradores. */
    public boolean puedeGestionar(Actor actor) {
        return actor != null && actor.canManage(autor.id());
    }

    /** Comprueba que el actor puede editar el banco ahora mismo (también antes de proponer una revisión). */
    public void exigirEditable(Actor actor) {
        exigirGestion(actor);
        exigirNoEliminado();
    }

    /** Cambia los datos directamente; ver {@link #edicionRequiereRevision}. */
    public void editar(DatosBanco nuevosDatos, Actor actor) {
        exigirEditable(actor);
        if (edicionRequiereRevision(actor)) {
            throw new IllegalStateException("La edición del autor de un banco publicado pasa por una revisión");
        }
        this.datos = Objects.requireNonNull(nuevosDatos, "nuevosDatos");
    }

    /**
     * Quien edita debe partir de la versión actual: si no, otra edición se ha guardado entretanto y
     * esta la pisaría. Los permisos se comprueban antes, para no responder con un conflicto de
     * versión a quien de todos modos no podría editar.
     */
    public void exigirVersion(long versionEsperada, Actor actor) {
        exigirGestion(actor);
        exigirNoEliminado();
        if (version != versionEsperada) {
            throw new PreconditionFailedException("BANCO_VERSION_OBSOLETA",
                    "El banco ha cambiado desde que lo cargaste; recárgalo y vuelve a intentarlo");
        }
    }

    public void eliminar(Actor actor) {
        exigirGestion(actor);
        exigirNoEliminado();
        estado = EstadoBanco.ELIMINADO;
    }

    public void aprobar(Instant ahora) {
        exigirNoEliminado();
        if (estado == EstadoBanco.PUBLICADO) {
            throw new BusinessRuleException("BANCO_YA_PUBLICADO", "El banco ya está publicado");
        }
        estado = EstadoBanco.PUBLICADO;
        fechaVerificacion = ahora;
    }

    public void rechazar() {
        exigirNoEliminado();
        if (estado == EstadoBanco.OCULTO) {
            throw new BusinessRuleException("BANCO_YA_OCULTO", "El banco ya está oculto");
        }
        estado = EstadoBanco.OCULTO;
        fechaVerificacion = null;
    }

    public void actualizarValoracion(Valoracion nuevaValoracion) {
        this.valoracion = Objects.requireNonNull(nuevaValoracion, "nuevaValoracion");
    }

    public boolean estaPublicado() {
        return estado == EstadoBanco.PUBLICADO;
    }

    private void exigirGestion(Actor actor) {
        if (!actor.canManage(autor.id())) {
            throw new ForbiddenException("BANCO_NO_AUTOR", "Solo el autor o un administrador pueden modificar este banco");
        }
    }

    /** Un banco eliminado no admite cambios ni contenido nuevo, aunque los administradores lo sigan viendo. */
    public void exigirNoEliminado() {
        if (estado == EstadoBanco.ELIMINADO) {
            throw new BusinessRuleException("BANCO_ELIMINADO", "El banco está eliminado");
        }
    }

    public UUID id() {
        return id;
    }

    public DatosBanco datos() {
        return datos;
    }

    public EstadoBanco estado() {
        return estado;
    }

    public Autor autor() {
        return autor;
    }

    public Valoracion valoracion() {
        return valoracion;
    }

    public Instant fechaSubida() {
        return fechaSubida;
    }

    public Instant fechaVerificacion() {
        return fechaVerificacion;
    }

    public long version() {
        return version;
    }
}
