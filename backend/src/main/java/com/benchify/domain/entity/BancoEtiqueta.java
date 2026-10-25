package com.benchify.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "banco_etiquetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BancoEtiqueta {

    @EmbeddedId
    private BancoEtiquetaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bancoId")
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("etiquetaId")
    @JoinColumn(name = "etiqueta_id", nullable = false)
    private Etiqueta etiqueta;

    // Auditoría: quién asignó la etiqueta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignada_por")
    private Usuario asignadaPor;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
    }
}
