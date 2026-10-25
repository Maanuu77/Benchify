package com.benchify.repository;

import com.benchify.domain.entity.Banco;
import com.benchify.domain.enums.EstadoBanco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BancoRepository extends JpaRepository<Banco, UUID> {

    /*
     * Devuelve todos los bancos PUBLICADOS dentro del bounding box visible del mapa.
     * Usa BETWEEN sobre el índice compuesto (lat, lng).
     * Portable entre PostgreSQL y H2 sin extensiones.
     */
    @Query("""
            SELECT b FROM Banco b
            WHERE b.estado = com.benchify.domain.enums.EstadoBanco.PUBLICADO
            AND b.lat BETWEEN :minLat AND :maxLat
            AND b.lng BETWEEN :minLng AND :maxLng
            """)
    List<Banco> findPublicadosEnBoundingBox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat
    );

    List<Banco> findByEstadoOrderByFechaSubidaDesc(EstadoBanco estado);

    List<Banco> findByCreadoPorIdOrderByFechaSubidaDesc(Long usuarioId);

    long countByEstado(EstadoBanco estado);
}
