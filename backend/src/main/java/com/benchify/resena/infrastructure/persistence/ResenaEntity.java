package com.benchify.resena.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.UUID;

/**
 * Los contadores de reportes nunca se escriben desde la entidad ({@code updatable = false}): solo
 * los cambian los UPDATE atómicos del adaptador. Así una edición hecha a partir de una lectura
 * anterior no puede devolverlos a un valor viejo.
 */
@Entity
@Table(name = "resenas")
@Getter
@Setter
@NoArgsConstructor
public class ResenaEntity {

    @Id
    private UUID id;

    @Column(name = "banco_id", nullable = false, updatable = false)
    private UUID bancoId;

    @Column(name = "usuario_id", nullable = false, updatable = false, length = 128)
    private String autorId;

    @Formula("(select u.nombre from usuarios u where u.id = usuario_id)")
    private String autorNombre;

    /** Estado del banco reseñado, para filtrar por visibilidad sin acoplarse a la feature de bancos. */
    @Formula("(select b.estado from bancos b where b.id = banco_id)")
    private String estadoBanco;

    @Column(nullable = false)
    private int puntuacion;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(name = "fecha_edicion")
    private Instant fechaEdicion;

    @Column(name = "num_reportes", nullable = false, updatable = false)
    private int numReportes;

    @Column(nullable = false, updatable = false)
    private boolean reportada;

    @Version
    private long version;
}
