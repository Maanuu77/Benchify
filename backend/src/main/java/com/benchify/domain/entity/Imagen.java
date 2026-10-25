package com.benchify.domain.entity;

import com.benchify.domain.enums.EstadoModeracion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imagenes", indexes = {
        @Index(name = "idx_imagenes_banco", columnList = "banco_id"),
        @Index(name = "idx_imagenes_estado", columnList = "estado_moderacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @Column(name = "url_r2", nullable = false)
    private String urlR2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subida_por", nullable = false)
    private Usuario subidaPor;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion", nullable = false, length = 20)
    @Builder.Default
    private EstadoModeracion estadoModeracion = EstadoModeracion.PENDIENTE;

    // Hash perceptual para detectar imágenes duplicadas
    @Column(name = "hash_perceptual", length = 64)
    private String hashPerceptual;

    @PrePersist
    protected void onCreate() {
        fechaSubida = LocalDateTime.now();
    }
}
