package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.EtiquetaRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.infrastructure.persistence.SpringDataPages;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class BancoPersistenceAdapter implements BancoRepository, EtiquetaRepository {

    private static final Sort MAS_RECIENTES = Sort.by(Sort.Direction.DESC, "fechaSubida").and(Sort.by("id"));
    private static final Sort MAS_ANTIGUOS = Sort.by(Sort.Direction.ASC, "fechaSubida").and(Sort.by("id"));

    private final BancoJpaRepository bancos;
    private final EtiquetaJpaRepository etiquetas;
    private final BancoSpatialQueries spatial;
    private final BancoEntityMapper mapper;
    private final EntityManager entityManager;

    BancoPersistenceAdapter(BancoJpaRepository bancos, EtiquetaJpaRepository etiquetas, BancoSpatialQueries spatial,
            BancoEntityMapper mapper, EntityManager entityManager) {
        this.bancos = bancos;
        this.etiquetas = etiquetas;
        this.spatial = spatial;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Banco save(Banco banco) {
        BancoEntity entity = bancos.findById(banco.id()).orElseGet(BancoEntity::new);
        boolean nuevo = entity.getId() == null;
        mapper.copyToEntity(banco, entity);
        entity.getEtiquetas().clear();
        banco.datos().etiquetas().forEach(etiqueta ->
                entity.getEtiquetas().add(entityManager.getReference(EtiquetaEntity.class, etiqueta.id())));
        if (nuevo) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        // Recarga columnas calculadas (@Formula autorNombre) y la versión incrementada
        entityManager.refresh(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Banco> findById(UUID id) {
        return bancos.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Banco> findByIdForUpdate(UUID id) {
        return bancos.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsPublicado(UUID id) {
        return bancos.existsByIdAndEstado(id, EstadoBanco.PUBLICADO);
    }

    @Override
    public List<Banco> findPublicadosEnArea(BoundingBox area, FiltroBancos filtro, int limite) {
        return cargarEnOrden(spatial.idsEnArea(area, filtro, limite));
    }

    @Override
    public PageResult<BancoCercano> findPublicadosCercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro,
            PageQuery page) {
        List<BancoSpatialQueries.IdConDistancia> resultados = spatial.idsCercanos(centro, radioMetros, filtro, page);
        if (resultados.isEmpty() && page.page() == 0) {
            return PageResult.empty(page);
        }
        Map<UUID, Banco> porId = cargarEnOrden(resultados.stream().map(BancoSpatialQueries.IdConDistancia::id).toList())
                .stream().collect(Collectors.toMap(Banco::id, Function.identity()));
        List<BancoCercano> contenido = resultados.stream()
                .filter(r -> porId.containsKey(r.id()))
                .map(r -> new BancoCercano(porId.get(r.id()), r.distancia()))
                .toList();
        return new PageResult<>(contenido, page.page(), page.size(), spatial.contarCercanos(centro, radioMetros, filtro));
    }

    @Override
    public PageResult<Banco> findByAutor(String autorId, boolean soloPublicados, PageQuery page) {
        Collection<EstadoBanco> estados = soloPublicados
                ? EnumSet.of(EstadoBanco.PUBLICADO)
                : EnumSet.complementOf(EnumSet.of(EstadoBanco.ELIMINADO));
        return SpringDataPages.result(
                bancos.findByCreadoPorAndEstadoIn(autorId, estados, SpringDataPages.pageable(page, MAS_RECIENTES)),
                mapper::toDomain);
    }

    @Override
    public PageResult<Banco> findByEstado(EstadoBanco estado, PageQuery page) {
        return SpringDataPages.result(bancos.findByEstado(estado, SpringDataPages.pageable(page, MAS_ANTIGUOS)),
                mapper::toDomain);
    }

    @Override
    public long countByEstado(EstadoBanco estado) {
        return bancos.countByEstado(estado);
    }

    @Override
    public List<Etiqueta> findAll() {
        return etiquetas.findAllByOrderByNombreAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Etiqueta> findAllById(Collection<Long> ids) {
        return etiquetas.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    /** {@code findAllById} no respeta el orden de la consulta espacial: se reordena aquí. */
    private List<Banco> cargarEnOrden(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, BancoEntity> porId = bancos.findAllById(ids).stream()
                .collect(Collectors.toMap(BancoEntity::getId, Function.identity()));
        return ids.stream().map(porId::get).filter(Objects::nonNull).map(mapper::toDomain).toList();
    }
}
