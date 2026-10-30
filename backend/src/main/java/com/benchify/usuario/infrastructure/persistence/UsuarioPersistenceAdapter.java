package com.benchify.usuario.infrastructure.persistence;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.persistence.SpringDataPages;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

@Component
class UsuarioPersistenceAdapter implements UsuarioRepository {

    private static final Sort MAS_RECIENTES = Sort.by(Sort.Direction.DESC, "fechaRegistro");

    private final UsuarioJpaRepository repository;
    private final UsuarioEntityMapper mapper;
    private final NamedParameterJdbcTemplate jdbc;

    UsuarioPersistenceAdapter(UsuarioJpaRepository repository, UsuarioEntityMapper mapper,
            NamedParameterJdbcTemplate jdbc) {
        this.repository = repository;
        this.mapper = mapper;
        this.jdbc = jdbc;
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
    public long count() {
        return repository.count();
    }

    /**
     * La SPA lanza varias peticiones a la vez en el primer inicio de sesión: con
     * {@code ON CONFLICT DO NOTHING} la segunda espera a que la primera confirme y no inserta nada,
     * en lugar de fallar por clave primaria duplicada.
     */
    @Override
    public Usuario registrarSiNoExiste(Usuario usuario) {
        jdbc.update("""
                INSERT INTO usuarios (id, email, nombre, avatar_url, ciudad, rol, fecha_registro, ultimo_acceso)
                VALUES (:id, :email, :nombre, :avatarUrl, :ciudad, :rol, :fechaRegistro, :ultimoAcceso)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", usuario.id())
                .addValue("email", usuario.email(), Types.VARCHAR)
                .addValue("nombre", usuario.nombre())
                .addValue("avatarUrl", usuario.avatarUrl(), Types.VARCHAR)
                .addValue("ciudad", usuario.ciudad(), Types.VARCHAR)
                .addValue("rol", usuario.rol().name())
                .addValue("fechaRegistro", Timestamp.from(usuario.fechaRegistro()))
                .addValue("ultimoAcceso", Timestamp.from(usuario.ultimoAcceso())));
        // Puede ser la fila de la otra petición: se lee de la base de datos en lugar de devolver la recibida
        return findById(usuario.id())
                .orElseThrow(() -> new IllegalStateException("User row missing after insert: " + usuario.id()));
    }

    @Override
    public void actualizarSincronizacion(Usuario usuario) {
        exigirFila(usuario.id(), repository.actualizarSincronizacion(
                usuario.id(), usuario.email(), usuario.avatarUrl(), usuario.ultimoAcceso()));
    }

    @Override
    public void actualizarPerfil(Usuario usuario) {
        exigirFila(usuario.id(), repository.actualizarPerfil(usuario.id(), usuario.nombre(), usuario.ciudad()));
    }

    @Override
    public void actualizarRol(String usuarioId, Rol rol) {
        exigirFila(usuarioId, repository.actualizarRol(usuarioId, rol));
    }

    @Override
    public boolean existeAdministrador() {
        return repository.existsByRol(Rol.ADMIN);
    }

    @Override
    public PageResult<Usuario> findAll(PageQuery page) {
        return SpringDataPages.result(repository.findAll(SpringDataPages.pageable(page, MAS_RECIENTES)),
                mapper::toDomain);
    }

    private static void exigirFila(String usuarioId, int filas) {
        if (filas == 0) {
            throw new UsuarioNotFoundException(usuarioId);
        }
    }
}
