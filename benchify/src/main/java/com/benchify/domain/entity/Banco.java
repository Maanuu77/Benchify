package com.benchify.domain.entity;

import com.benchify.domain.enums.EstadoBanco;
import com.benchify.domain.enums.MaterialBanco;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bancos", indexes = {
        @Index(name = "idx_bancos_estado", columnList = "estado"),
        @Index(name = "idx_bancos_ciudad", columnList = "ciudad"),
        @Index(name = "idx_bancos_creado_por", columnList = "creado_por"),
        @Index(name = "idx_bancos_coords", columnList = "lat, lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String titulo;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion;

    /*
     * Coordenadas WGS84 separadas en columnas escalares.
     * Sirve perfectamente para MVP (<100k bancos) con índice compuesto B-tree.
     * Si en producción se necesita PostGIS, se reintroduce un Point geográfico.
     */
    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    @Column(nullable = false)
    private Double lat;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    @Column(nullable = false)
    private Double lng;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    @Builder.Default
    private String pais = "España";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoBanco estado = EstadoBanco.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "con_sombra", nullable = false)
    @Builder.Default
    private boolean conSombra = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean accesible = false;

    @Column(name = "con_respaldo", nullable = false)
    @Builder.Default
    private boolean conRespaldo = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaterialBanco material;

    // Campos denormalizados para rendimiento — se actualizan en ResenaService
    @Column(name = "puntuacion_media", nullable = false)
    @Builder.Default
    private double puntuacionMedia = 0.0;

    @Column(name = "num_resenas", nullable = false)
    @Builder.Default
    private int numResenas = 0;

    @OneToMany(mappedBy = "banco", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BancoEtiqueta> bancoEtiquetas = new ArrayList<>();

    @OneToMany(mappedBy = "banco", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Imagen> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "banco", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Resena> resenas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaSubida = LocalDateTime.now();
    }
}
