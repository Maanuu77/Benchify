package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoImagen;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ImagenJpaRepository extends JpaRepository<ImagenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImagenEntity i where i.id = :id")
    Optional<ImagenEntity> findByIdForUpdate(@Param("id") UUID id);

    List<ImagenEntity> findByBancoIdAndEstadoNotOrderByFechaSubidaAscIdAsc(UUID bancoId, EstadoImagen estado);

    long countByBancoIdAndEstadoNot(UUID bancoId, EstadoImagen estado);

    /** Bloquea las imágenes del banco: así se leen las rutas vigentes aunque se esté aprobando alguna. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImagenEntity i where i.bancoId = :bancoId")
    List<ImagenEntity> findByBancoIdForUpdate(@Param("bancoId") UUID bancoId);

    /** La huella no está en la entidad: solo se escribe al subir la foto y solo se usa en esta consulta. */
    @Modifying
    @Query(value = "UPDATE imagenes SET huella = :huella WHERE id = :id", nativeQuery = true)
    void guardarHuella(@Param("id") UUID id, @Param("huella") long huella);

    /** Distancia de Hamming entre huellas: bits a 1 del XOR ({@code #}) de las dos. */
    @Query(value = """
            SELECT i.* FROM imagenes i
            JOIN imagenes o ON o.id = :id AND o.huella IS NOT NULL
            WHERE i.id <> o.id AND i.huella IS NOT NULL
              AND bit_count((i.huella # o.huella)::bit(64)) <= :distancia
            ORDER BY bit_count((i.huella # o.huella)::bit(64)), i.fecha_subida, i.id
            LIMIT :limite
            """, nativeQuery = true)
    List<ImagenEntity> findParecidas(@Param("id") UUID id, @Param("distancia") int distancia,
            @Param("limite") int limite);

    Page<ImagenEntity> findByEstado(EstadoImagen estado, Pageable pageable);

    long countByEstado(EstadoImagen estado);
}
