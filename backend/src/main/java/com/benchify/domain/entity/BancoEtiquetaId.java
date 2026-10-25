package com.benchify.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BancoEtiquetaId implements Serializable {

    @Column(name = "banco_id")
    private UUID bancoId;

    @Column(name = "etiqueta_id")
    private Long etiquetaId;
}
