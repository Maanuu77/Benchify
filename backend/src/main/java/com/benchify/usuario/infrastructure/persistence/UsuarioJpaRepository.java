package com.benchify.usuario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, String> {
}
