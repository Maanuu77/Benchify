package com.benchify.banco.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/** Catálogo de solo lectura: las etiquetas se añaden con migraciones Flyway. */
@Entity
@Table(name = "etiquetas")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class EtiquetaEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false, length = 50)
    private String icono;
}
