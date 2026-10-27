package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.Rol;
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

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @Column(length = 128)
    private String id;

    private String email;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(length = 100)
    private String ciudad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "ultimo_acceso", nullable = false)
    private Instant ultimoAcceso;
}
