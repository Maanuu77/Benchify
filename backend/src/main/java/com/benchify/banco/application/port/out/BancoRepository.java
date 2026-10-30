package com.benchify.banco.application.port.out;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de bancos. Otras features (reseñas, moderación) también lo usan. */
public interface BancoRepository {

    Banco save(Banco banco);

    Optional<Banco> findById(UUID id);

    /**
     * Lee el banco bloqueando su fila hasta el final de la transacción. Lo usan las reseñas para
     * recalcular la valoración sin perder actualizaciones concurrentes.
     */
    Optional<Banco> findByIdForUpdate(UUID id);

    /** Carga varios bancos respetando el orden de {@code ids}; los inexistentes se omiten. */
    List<Banco> findAllById(List<UUID> ids);

    /** Publicados dentro del área, mejor valorados primero. */
    List<Banco> findPublicadosEnArea(BoundingBox area, FiltroBancos filtro, int limite);

    /** Publicados a menos de {@code radioMetros}, del más cercano al más lejano. */
    PageResult<BancoCercano> findPublicadosCercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro,
            PageQuery page);

    /** Bancos de un autor (excepto eliminados), del más reciente al más antiguo. */
    PageResult<Banco> findByAutor(String autorId, boolean soloPublicados, PageQuery page);

    /** Bancos en un estado, del más antiguo al más reciente (cola de moderación). */
    PageResult<Banco> findByEstado(EstadoBanco estado, PageQuery page);

    long countByEstado(EstadoBanco estado);

    /**
     * Bancos pendientes de moderación de un autor. Antes bloquea sus propuestas hasta el final de
     * la transacción: dos propuestas simultáneas no pueden pasar a la vez la comprobación del
     * límite y guardarse las dos.
     */
    long countPendientesByAutorForUpdate(String autorId);
}
