package com.benchify.banco.application.service;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.application.port.in.GestionarBancoUseCase;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.EtiquetaRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BancoService implements GestionarBancoUseCase, ConsultarBancosUseCase {

    private final BancoRepository bancos;
    private final EtiquetaRepository etiquetas;
    private final Clock clock;

    public BancoService(BancoRepository bancos, EtiquetaRepository etiquetas, Clock clock) {
        this.bancos = bancos;
        this.etiquetas = etiquetas;
        this.clock = clock;
    }

    @Override
    public Banco proponer(Actor actor, BancoCommand command) {
        Banco banco = Banco.proponer(UUID.randomUUID(), toDatos(command), actor, clock.instant());
        return bancos.save(banco);
    }

    @Override
    public Banco editar(Actor actor, UUID bancoId, BancoCommand command) {
        Banco banco = cargarVisible(bancoId, actor);
        banco.editar(toDatos(command), actor);
        return bancos.save(banco);
    }

    @Override
    public void eliminar(Actor actor, UUID bancoId) {
        Banco banco = cargarVisible(bancoId, actor);
        banco.eliminar(actor);
        bancos.save(banco);
    }

    @Override
    @Transactional(readOnly = true)
    public Banco obtener(UUID bancoId, Actor actor) {
        return cargarVisible(bancoId, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banco> enArea(BoundingBox area, FiltroBancos filtro) {
        return bancos.findPublicadosEnArea(area, filtro, MAX_RESULTADOS_MAPA);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BancoCercano> cercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro, PageQuery page) {
        if (radioMetros < 1 || radioMetros > RADIO_MAXIMO_METROS) {
            throw new BusinessRuleException("RADIO_INVALIDO",
                    "El radio debe estar entre 1 y " + RADIO_MAXIMO_METROS + " metros");
        }
        return bancos.findPublicadosCercanos(centro, radioMetros, filtro, page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Banco> deUsuario(String usuarioId, Actor actor, PageQuery page) {
        boolean soloPublicados = actor == null || !actor.canManage(usuarioId);
        return bancos.findByAutor(usuarioId, soloPublicados, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Etiqueta> etiquetas() {
        return etiquetas.findAll();
    }

    /** Un banco que el actor no puede ver se trata como inexistente para no revelar su existencia. */
    private Banco cargarVisible(UUID bancoId, Actor actor) {
        return bancos.findById(bancoId)
                .filter(banco -> banco.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
    }

    private DatosBanco toDatos(BancoCommand command) {
        return new DatosBanco(
                command.titulo(),
                command.descripcion(),
                new Ubicacion(command.lat(), command.lng()),
                new DireccionPostal(command.direccion(), command.ciudad(), command.provincia(), command.pais()),
                new Caracteristicas(command.conSombra(), command.accesible(), command.conRespaldo(), command.material()),
                resolverEtiquetas(command.etiquetaIds()));
    }

    private Set<Etiqueta> resolverEtiquetas(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Etiqueta> encontradas = new HashSet<>(etiquetas.findAllById(ids));
        if (encontradas.size() != ids.size()) {
            Set<Long> desconocidas = new HashSet<>(ids);
            desconocidas.removeAll(encontradas.stream().map(Etiqueta::id).collect(Collectors.toSet()));
            throw new BusinessRuleException("ETIQUETA_DESCONOCIDA", "Etiquetas desconocidas: " + desconocidas);
        }
        return encontradas;
    }
}
