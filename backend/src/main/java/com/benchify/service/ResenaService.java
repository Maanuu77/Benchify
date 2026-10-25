package com.benchify.service;

import com.benchify.domain.entity.Banco;
import com.benchify.domain.entity.Resena;
import com.benchify.domain.entity.Usuario;
import com.benchify.domain.enums.EstadoBanco;
import com.benchify.dto.ResenaRequest;
import com.benchify.dto.ResenaResponse;
import com.benchify.exception.DuplicateResenaException;
import com.benchify.exception.ResourceNotFoundException;
import com.benchify.repository.BancoRepository;
import com.benchify.repository.ResenaRepository;
import com.benchify.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final BancoRepository bancoRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaResponse crear(UUID bancoId, ResenaRequest request, String email) {
        Banco banco = bancoRepository.findById(bancoId)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + bancoId));

        if (banco.getEstado() != EstadoBanco.PUBLICADO) {
            throw new IllegalStateException("Solo se pueden reseñar bancos publicados");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (resenaRepository.existsByBancoIdAndUsuarioId(bancoId, usuario.getId())) {
            throw new DuplicateResenaException("Ya has dejado una reseña en este banco");
        }

        Resena resena = Resena.builder()
                .banco(banco)
                .usuario(usuario)
                .puntuacion(request.getPuntuacion())
                .comentario(request.getComentario())
                .build();

        resenaRepository.save(resena);
        actualizarEstadisticasBanco(banco);

        return toResponse(resena);
    }

    public void reportar(Long resenaId) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada: " + resenaId));

        resena.setNumReportes(resena.getNumReportes() + 1);
        if (resena.getNumReportes() >= 3) {
            resena.setReportada(true);
        }
        resenaRepository.save(resena);
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerPorBanco(UUID bancoId) {
        return resenaRepository.findByBancoIdOrderByFechaResenaDesc(bancoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void actualizarEstadisticasBanco(Banco banco) {
        Double media = resenaRepository.calcularMediaPuntuacion(banco.getId());
        long count = resenaRepository.countByBancoId(banco.getId());
        banco.setPuntuacionMedia(media != null ? Math.round(media * 10.0) / 10.0 : 0.0);
        banco.setNumResenas((int) count);
        bancoRepository.save(banco);
    }

    public ResenaResponse toResponse(Resena resena) {
        return ResenaResponse.builder()
                .id(resena.getId())
                .puntuacion(resena.getPuntuacion())
                .comentario(resena.getComentario())
                .fechaResena(resena.getFechaResena())
                .nombreUsuario(resena.getUsuario().getNombre())
                .avatarUrl(resena.getUsuario().getAvatarUrl())
                .reportada(resena.isReportada())
                .numReportes(resena.getNumReportes())
                .build();
    }
}
