package com.benchify.service;

import com.benchify.domain.entity.*;
import com.benchify.domain.enums.EstadoBanco;
import com.benchify.dto.BancoMapaResponse;
import com.benchify.dto.BancoRequest;
import com.benchify.dto.BancoResponse;
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
public class BancoService {

    private final BancoRepository bancoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final BancoEtiquetaRepository bancoEtiquetaRepository;
    private final BancoMapper bancoMapper;

    @Transactional(readOnly = true)
    public List<BancoMapaResponse> obtenerEnBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
        return bancoRepository.findPublicadosEnBoundingBox(minLng, minLat, maxLng, maxLat)
                .stream()
                .map(bancoMapper::toMapaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BancoResponse obtenerPorId(UUID id) {
        Banco banco = bancoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + id));
        return bancoMapper.toResponse(banco);
    }

    public BancoResponse crear(BancoRequest request, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Banco banco = Banco.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .lat(request.getLat())
                .lng(request.getLng())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .provincia(request.getProvincia())
                .pais(request.getPais() != null ? request.getPais() : "España")
                .conSombra(request.isConSombra())
                .accesible(request.isAccesible())
                .conRespaldo(request.isConRespaldo())
                .material(request.getMaterial())
                .creadoPor(usuario)
                .build();

        Banco saved = bancoRepository.save(banco);

        if (request.getEtiquetaIds() != null && !request.getEtiquetaIds().isEmpty()) {
            asignarEtiquetas(saved, request.getEtiquetaIds(), usuario);
        }

        return bancoMapper.toResponse(saved);
    }

    public BancoResponse actualizar(UUID id, BancoRequest request, String email) {
        Banco banco = bancoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + id));

        verificarPropietario(banco, email);

        banco.setTitulo(request.getTitulo());
        banco.setDescripcion(request.getDescripcion());
        banco.setDireccion(request.getDireccion());
        banco.setCiudad(request.getCiudad());
        banco.setProvincia(request.getProvincia());
        banco.setConSombra(request.isConSombra());
        banco.setAccesible(request.isAccesible());
        banco.setConRespaldo(request.isConRespaldo());
        banco.setMaterial(request.getMaterial());

        if (request.getLat() != null && request.getLng() != null) {
            banco.setLat(request.getLat());
            banco.setLng(request.getLng());
        }

        if (request.getEtiquetaIds() != null) {
            bancoEtiquetaRepository.deleteByBancoId(banco.getId());
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            asignarEtiquetas(banco, request.getEtiquetaIds(), usuario);
        }

        return bancoMapper.toResponse(bancoRepository.save(banco));
    }

    public void eliminar(UUID id, String email) {
        Banco banco = bancoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado: " + id));
        verificarPropietario(banco, email);
        banco.setEstado(EstadoBanco.ELIMINADO);
        bancoRepository.save(banco);
    }

    @Transactional(readOnly = true)
    public List<BancoResponse> obtenerPorUsuario(Long usuarioId) {
        return bancoRepository.findByCreadoPorIdOrderByFechaSubidaDesc(usuarioId)
                .stream()
                .map(bancoMapper::toResponse)
                .toList();
    }

    private void asignarEtiquetas(Banco banco, List<Long> etiquetaIds, Usuario usuario) {
        etiquetaIds.forEach(etiquetaId ->
            etiquetaRepository.findById(etiquetaId).ifPresent(etiqueta -> {
                BancoEtiquetaId beId = new BancoEtiquetaId(banco.getId(), etiqueta.getId());
                BancoEtiqueta be = BancoEtiqueta.builder()
                        .id(beId)
                        .banco(banco)
                        .etiqueta(etiqueta)
                        .asignadaPor(usuario)
                        .build();
                bancoEtiquetaRepository.save(be);
            })
        );
    }

    private void verificarPropietario(Banco banco, String email) {
        if (!banco.getCreadoPor().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para modificar este banco");
        }
    }
}
