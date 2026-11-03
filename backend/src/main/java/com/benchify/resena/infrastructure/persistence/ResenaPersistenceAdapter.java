package com.benchify.resena.infrastructure.persistence;

import com.benchify.banco.domain.EstadoBanco;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResenaNotFoundException;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.infrastructure.persistence.SpringDataPages;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class ResenaPersistenceAdapter implements ResenaRepository {

    private static final Sort MAS_RECIENTES = Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by("id"));
    private static final Sort MAS_REPORTADAS = Sort.by(Sort.Direction.DESC, "numReportes")
            .and(Sort.by("fecha")).and(Sort.by("id"));

    private final ResenaJpaRepository repository;
    private final ResenaEntityMapper mapper;
    private final EntityManager entityManager;

    ResenaPersistenceAdapter(ResenaJpaRepository repository, ResenaEntityMapper mapper, EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    /**
     * Una reseña existente se actualiza sobre la entidad gestionada para que Hibernate compruebe
     * la versión al volcarla ({@code UPDATE … WHERE version = ?}); además se compara con la versión
     * que trae el dominio por si la reseña se leyó en otra transacción.
     */
    @Override
    public Resena save(Resena resena) {
        ResenaEntity entity = repository.findById(resena.id()).orElse(null);
        if (entity == null) {
            entity = mapper.toEntity(resena);
            entityManager.persist(entity);
        } else {
            if (entity.getVersion() != resena.version()) {
                throw new ObjectOptimisticLockingFailureException(ResenaEntity.class, resena.id());
            }
            mapper.copiarEdicion(resena, entity);
        }
        entityManager.flush();
        // Recarga el nombre del autor (@Formula), la versión incrementada y los contadores de reportes
        entityManager.refresh(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Resena> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Resena> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
        repository.flush();
    }

    @Override
    public boolean existsByBancoAndAutor(UUID bancoId, String autorId) {
        return repository.existsByBancoIdAndAutorId(bancoId, autorId);
    }

    @Override
    public Optional<Resena> findByBancoAndAutor(UUID bancoId, String autorId) {
        return repository.findByBancoIdAndAutorId(bancoId, autorId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Resena> findByBanco(UUID bancoId, PageQuery page) {
        return SpringDataPages.result(repository.findByBancoId(bancoId, SpringDataPages.pageable(page, MAS_RECIENTES)),
                mapper::toDomain);
    }

    @Override
    public PageResult<Resena> findByAutorEnBancosPublicados(String autorId, PageQuery page) {
        return SpringDataPages.result(repository.findByAutorIdAndEstadoBanco(autorId, EstadoBanco.PUBLICADO.name(),
                SpringDataPages.pageable(page, MAS_RECIENTES)), mapper::toDomain);
    }

    @Override
    public PageResult<Resena> findReportadas(PageQuery page) {
        return SpringDataPages.result(repository.findByReportadaTrue(SpringDataPages.pageable(page, MAS_REPORTADAS)),
                mapper::toDomain);
    }

    @Override
    public long countReportadas() {
        return repository.countByReportadaTrue();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public boolean registrarReporte(UUID resenaId, String usuarioId, Instant fecha) {
        int filas = entityManager.createNativeQuery("""
                        INSERT INTO resena_reportes (resena_id, usuario_id, fecha) VALUES (:resena, :usuario, :fecha)
                        ON CONFLICT DO NOTHING
                        """)
                .setParameter("resena", resenaId)
                .setParameter("usuario", usuarioId)
                .setParameter("fecha", fecha)
                .executeUpdate();
        return filas == 1;
    }

    /** En el SET, {@code num_reportes} es el valor anterior a la actualización. */
    @Override
    public Resena sumarReporte(UUID resenaId, int umbral) {
        entityManager.createNativeQuery("""
                        UPDATE resenas
                        SET num_reportes = num_reportes + 1,
                            reportada = reportada OR num_reportes + 1 >= :umbral
                        WHERE id = :resena
                        """)
                .setParameter("umbral", umbral)
                .setParameter("resena", resenaId)
                .executeUpdate();
        return recargar(resenaId);
    }

    @Override
    public Resena borrarReportes(UUID resenaId) {
        entityManager.createNativeQuery("DELETE FROM resena_reportes WHERE resena_id = :resena")
                .setParameter("resena", resenaId)
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE resenas SET num_reportes = 0, reportada = FALSE WHERE id = :resena")
                .setParameter("resena", resenaId)
                .executeUpdate();
        return recargar(resenaId);
    }

    @Override
    public ResumenPuntuacion resumen(UUID bancoId) {
        ResenaJpaRepository.Resumen resumen = repository.resumen(bancoId);
        return resumen.total() == 0
                ? ResumenPuntuacion.VACIO
                : new ResumenPuntuacion(resumen.media(), resumen.total().intValue());
    }

    /** Las sentencias nativas no actualizan la entidad que ya estuviera en la sesión: se refresca. */
    private Resena recargar(UUID resenaId) {
        ResenaEntity entity = Optional.ofNullable(entityManager.find(ResenaEntity.class, resenaId))
                .orElseThrow(() -> new ResenaNotFoundException(resenaId));
        entityManager.refresh(entity);
        return mapper.toDomain(entity);
    }
}
