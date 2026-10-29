package com.benchify.checkin.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.checkin.application.port.in.CheckInConBanco;
import com.benchify.checkin.application.port.in.CheckInsUseCase;
import com.benchify.checkin.application.port.out.CheckInRepository;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.checkin.domain.PoliticaCheckIn;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CheckInService implements CheckInsUseCase {

    private final CheckInRepository checkIns;
    private final BancoRepository bancos;
    private final PoliticaCheckIn politica;
    private final Clock clock;

    public CheckInService(CheckInRepository checkIns, BancoRepository bancos, PoliticaCheckIn politica, Clock clock) {
        this.checkIns = checkIns;
        this.bancos = bancos;
        this.politica = politica;
        this.clock = clock;
    }

    /**
     * Leer el último check-in bloquea la pareja (usuario, banco) hasta que esta transacción
     * termina, así que la comprobación del intervalo y la inserción son atómicas.
     */
    @Override
    public CheckIn registrar(Actor actor, UUID bancoId, Ubicacion posicion) {
        Banco banco = bancos.findById(bancoId)
                .filter(b -> b.esVisiblePara(actor))
                .orElseThrow(() -> new BancoNotFoundException(bancoId));
        if (!banco.estaPublicado()) {
            throw new BusinessRuleException("BANCO_NO_PUBLICADO", "Solo se puede hacer check-in en bancos publicados");
        }
        CheckIn checkIn = CheckIn.registrar(UUID.randomUUID(), actor.userId(), bancoId, banco.datos().ubicacion(),
                posicion, checkIns.findUltimaFechaForUpdate(actor.userId(), bancoId), politica, clock.instant());
        return checkIns.save(checkIn);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CheckInConBanco> misCheckIns(Actor actor, PageQuery page) {
        PageResult<CheckIn> pagina = checkIns.findByUsuario(actor.userId(), page);
        List<UUID> ids = pagina.content().stream().map(CheckIn::bancoId).distinct().toList();
        Map<UUID, String> titulos = bancos.findAllById(ids).stream()
                .collect(Collectors.toMap(Banco::id, banco -> banco.datos().titulo()));
        return pagina.map(checkIn -> new CheckInConBanco(checkIn, titulos.get(checkIn.bancoId())));
    }

    @Override
    @Transactional(readOnly = true)
    public long totalDeBanco(UUID bancoId) {
        return checkIns.countByBanco(bancoId);
    }
}
