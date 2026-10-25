package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.infrastructure.persistence.SpringDataPages;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.Usuario;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UsuarioPersistenceAdapter implements UsuarioRepository {

    private static final Sort MAS_RECIENTES = Sort.by(Sort.Direction.DESC, "fechaRegistro");

    private final UsuarioJpaRepository repository;
    private final UsuarioEntityMapper mapper;

    UsuarioPersistenceAdapter(UsuarioJpaRepository repository, UsuarioEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Usuario> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return mapper.toDomain(repository.save(mapper.toEntity(usuario)));
    }

    @Override
    public PageResult<Usuario> findAll(PageQuery page) {
        return SpringDataPages.result(repository.findAll(SpringDataPages.pageable(page, MAS_RECIENTES)),
                mapper::toDomain);
    }
}
