package com.benchify.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "etiquetas", indexes = {
        @Index(name = "idx_etiquetas_nombre", columnList = "nombre", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true, nullable = false, length = 50)
    private String nombre;

    // Código hexadecimal del color (#RRGGBB)
    @Column(length = 7)
    private String color;

    // Identificador del icono (nombre de clase CSS o clave de icon set)
    @Column(length = 50)
    private String icono;
}
