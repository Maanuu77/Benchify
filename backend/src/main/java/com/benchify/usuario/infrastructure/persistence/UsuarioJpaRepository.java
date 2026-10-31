package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Las actualizaciones son {@code UPDATE} de columnas concretas y no un {@code save} de la entidad
 * entera, que escribiría también las columnas que no han cambiado con el valor leído al principio.
 */
interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, String> {

    boolean existsByRol(Rol rol);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UsuarioEntity u set u.email = :email, u.avatarUrl = :avatarUrl, u.ultimoAcceso = :ultimoAcceso
            where u.id = :id
            """)
    int actualizarSincronizacion(@Param("id") String id, @Param("email") String email,
            @Param("avatarUrl") String avatarUrl, @Param("ultimoAcceso") Instant ultimoAcceso);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UsuarioEntity u set u.nombre = :nombre, u.ciudad = :ciudad where u.id = :id")
    int actualizarPerfil(@Param("id") String id, @Param("nombre") String nombre, @Param("ciudad") String ciudad);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UsuarioEntity u set u.rol = :rol where u.id = :id")
    int actualizarRol(@Param("id") String id, @Param("rol") Rol rol);
}
