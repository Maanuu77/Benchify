package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoImagen;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "imagenes")
@Getter
@Setter
@NoArgsConstructor
public class ImagenEntity {

    @Id
    private UUID id;

    @Column(name = "banco_id", nullable = false, updatable = false)
    private UUID bancoId;

    @Column(nullable = false, updatable = false, length = 512)
    private String ruta;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "subida_por", nullable = false, updatable = false, length = 128)
    private String subidaPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoImagen estado;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private Instant fechaSubida;
}
