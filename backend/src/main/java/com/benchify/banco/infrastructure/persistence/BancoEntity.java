package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.MaterialBanco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * La columna {@code ubicacion} (geography) no se mapea: la genera PostgreSQL a partir de
 * {@code lat}/{@code lng} y solo se usa en las consultas espaciales nativas.
 */
@Entity
@Table(name = "bancos")
@Getter
@Setter
@NoArgsConstructor
public class BancoEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String provincia;

    @Column(nullable = false, length = 100)
    private String pais;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBanco estado;

    @Column(name = "con_sombra", nullable = false)
    private boolean conSombra;

    @Column(nullable = false)
    private boolean accesible;

    @Column(name = "con_respaldo", nullable = false)
    private boolean conRespaldo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaterialBanco material;

    @Column(name = "puntuacion_media", nullable = false)
    private double puntuacionMedia;

    @Column(name = "num_resenas", nullable = false)
    private int numResenas;

    @Column(name = "creado_por", nullable = false, updatable = false, length = 128)
    private String creadoPor;

    /** Nombre del autor para mostrarlo sin acoplar esta feature a la de usuarios. */
    @Formula("(select u.nombre from usuarios u where u.id = creado_por)")
    private String autorNombre;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private Instant fechaSubida;

    @Column(name = "fecha_verificacion")
    private Instant fechaVerificacion;

    @Version
    private long version;

    @ManyToMany
    @JoinTable(name = "banco_etiquetas",
            joinColumns = @JoinColumn(name = "banco_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private Set<EtiquetaEntity> etiquetas = new HashSet<>();
}
