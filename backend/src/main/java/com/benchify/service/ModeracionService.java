package com.benchify.service;

import com.benchify.domain.entity.Banco;
import com.benchify.domain.enums.EstadoBanco;
import com.benchify.dto.BancoResponse;
import com.benchify.dto.EstadisticasResponse;
import com.benchify.dto.ResenaResponse;
import com.benchify.exception.ResourceNotFoundException;
import com.benchify.mapper.BancoMapper;
import com.benchify.repository.BancoRepository;
import com.benchify.repository.ResenaRepository;
import com.benchify.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ModeracionService {

    private final BancoRepository bancoRepository;
    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BancoMapper bancoMapper;
    private final ResenaService resenaService;

    @Transactional(readOnly = true)
    public List<BancoResponse> obtenerBancosPendientes() {
        return bancoRepository.findByEstadoOrderByFechaSubidaDesc(EstadoBanco.PENDIENTE)
                .stream()
                .map(bancoMapper::toResponse)
                .toList();
    }

    public void aprobarBanco(UUID bancoId) {
        Banco banco = bancoRepository.findById(bancoId)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + bancoId));
        banco.setEstado(EstadoBanco.PUBLICADO);
        banco.setFechaVerificacion(LocalDateTime.now());
        bancoRepository.save(banco);
    }

    public void rechazarBanco(UUID bancoId) {
        Banco banco = bancoRepository.findById(bancoId)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + bancoId));
        banco.setEstado(EstadoBanco.OCULTO);
        bancoRepository.save(banco);
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerResenasPendientesModeracion() {
        return resenaRepository.findByReportadaTrueOrderByNumReportesDesc()
                .stream()
                .map(resenaService::toResponse)
                .toList();
    }

    public void eliminarResena(Long resenaId) {
        if (!resenaRepository.existsById(resenaId)) {
            throw new ResourceNotFoundException("Reseña no encontrada: " + resenaId);
        }
        resenaRepository.deleteById(resenaId);
    }

    @Transactional(readOnly = true)
    public EstadisticasResponse obtenerEstadisticas() {
        return EstadisticasResponse.builder()
                .totalBancos(bancoRepository.count())
                .bancosPendientes(bancoRepository.countByEstado(EstadoBanco.PENDIENTE))
                .bancosPublicados(bancoRepository.countByEstado(EstadoBanco.PUBLICADO))
                .bancosOcultos(bancoRepository.countByEstado(EstadoBanco.OCULTO))
                .totalUsuarios(usuarioRepository.count())
                .totalResenas(resenaRepository.count())
                .resenasPendientesModeracion(resenaRepository.findByReportadaTrueOrderByNumReportesDesc().size())
                .build();
    }
}
