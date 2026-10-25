package com.benchify.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resenas", uniqueConstraints = {
        // Un usuario solo puede dejar una reseña por banco
        @UniqueConstraint(name = "uk_resena_banco_usuario", columnNames = {"banco_id", "usuario_id"})
}, indexes = {
        @Index(name = "idx_resenas_banco", columnList = "banco_id"),
        @Index(name = "idx_resenas_reportada", columnList = "reportada")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer puntuacion;

    @Size(max = 1000)
    @Column(length = 1000)
    private String comentario;

    @Column(name = "fecha_resena", nullable = false, updatable = false)
    private LocalDateTime fechaResena;

    @Column(nullable = false)
    @Builder.Default
    private boolean reportada = false;

    @Column(name = "num_reportes", nullable = false)
    @Builder.Default
    private int numReportes = 0;

    @PrePersist
    protected void onCreate() {
        fechaResena = LocalDateTime.now();
    }
}
