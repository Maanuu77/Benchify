package com.benchify.resena.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.UUID;

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

    @Column(nullable = false)
    private int puntuacion;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(name = "fecha_edicion")
    private Instant fechaEdicion;

    @Column(name = "num_reportes", nullable = false)
    private int numReportes;

    @Column(nullable = false)
    private boolean reportada;
}
