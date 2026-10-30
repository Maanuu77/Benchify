package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.infrastructure.persistence.SpringDataPages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ImagenPersistenceAdapter implements ImagenRepository {

    private static final Sort MAS_ANTIGUAS = Sort.by("fechaSubida", "id");

    private final ImagenJpaRepository repository;
    private final ImagenEntityMapper mapper;

    ImagenPersistenceAdapter(ImagenJpaRepository repository, ImagenEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Imagen save(Imagen imagen) {
        return mapper.toDomain(repository.save(mapper.toEntity(imagen)));
    }

    @Override
    public Imagen saveAndFlush(Imagen imagen) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(imagen)));
    }

    @Override
    public Optional<Imagen> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Imagen> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Imagen> findActivasByBanco(UUID bancoId) {
        return repository.findByBancoIdAndEstadoNotOrderByFechaSubidaAscIdAsc(bancoId, EstadoImagen.RECHAZADA)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countActivasByBanco(UUID bancoId) {
        return repository.countByBancoIdAndEstadoNot(bancoId, EstadoImagen.RECHAZADA);
    }

    @Override
    public PageResult<Imagen> findByEstado(EstadoImagen estado, PageQuery page) {
        return SpringDataPages.result(repository.findByEstado(estado, SpringDataPages.pageable(page, MAS_ANTIGUAS)),
                mapper::toDomain);
    }

    @Override
    public long countByEstado(EstadoImagen estado) {
        return repository.countByEstado(estado);
    }
}
