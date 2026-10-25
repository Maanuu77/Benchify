package com.benchify.service;

import com.benchify.domain.entity.CheckIn;
import com.benchify.domain.entity.Favorito;
import com.benchify.domain.entity.Usuario;
import com.benchify.dto.BancoResponse;
import com.benchify.dto.PerfilResponse;
import com.benchify.exception.ResourceNotFoundException;
import com.benchify.mapper.BancoMapper;
import com.benchify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BancoRepository bancoRepository;
    private final FavoritoRepository favoritoRepository;
    private final CheckInRepository checkInRepository;
    private final BancoMapper bancoMapper;

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        List<BancoResponse> bancosRecientes = bancoRepository
                .findByCreadoPorIdOrderByFechaSubidaDesc(usuarioId)
                .stream()
                .limit(5)
                .map(bancoMapper::toResponse)
                .toList();

        return PerfilResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .avatarUrl(usuario.getAvatarUrl())
                .ciudad(usuario.getCiudad())
                .numBancosSubidos(bancoRepository.findByCreadoPorIdOrderByFechaSubidaDesc(usuarioId).size())
                .numResenas(usuario.getResenas().size())
                .numFavoritos(favoritoRepository.countByUsuarioId(usuarioId))
                .numCheckIns(checkInRepository.countByUsuarioId(usuarioId))
                .bancosRecientes(bancosRecientes)
                .build();
    }

    public boolean toggleFavorito(UUID bancoId, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        var banco = bancoRepository.findById(bancoId)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + bancoId));

        return favoritoRepository.findByUsuarioIdAndBancoId(usuario.getId(), bancoId)
                .map(fav -> {
                    favoritoRepository.delete(fav);
                    return false; // eliminado de favoritos
                })
                .orElseGet(() -> {
                    favoritoRepository.save(Favorito.builder().usuario(usuario).banco(banco).build());
                    return true; // añadido a favoritos
                });
    }

    public void registrarCheckin(UUID bancoId, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        var banco = bancoRepository.findById(bancoId)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + bancoId));

        checkInRepository.save(CheckIn.builder().usuario(usuario).banco(banco).build());
    }

    @Transactional(readOnly = true)
    public boolean esFavorito(UUID bancoId, String email) {
        return usuarioRepository.findByEmail(email)
                .map(u -> favoritoRepository.existsByUsuarioIdAndBancoId(u.getId(), bancoId))
                .orElse(false);
    }
}
