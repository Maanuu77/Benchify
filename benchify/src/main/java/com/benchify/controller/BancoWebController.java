package com.benchify.controller;

import com.benchify.dto.BancoResponse;
import com.benchify.dto.ResenaResponse;
import com.benchify.repository.EtiquetaRepository;
import com.benchify.service.BancoService;
import com.benchify.service.ResenaService;
import com.benchify.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BancoWebController {

    private final BancoService bancoService;
    private final ResenaService resenaService;
    private final UsuarioService usuarioService;
    private final EtiquetaRepository etiquetaRepository;

    @GetMapping("/bancos/nuevo")
    public String nuevoBanco(Model model) {
        model.addAttribute("etiquetas", etiquetaRepository.findAllByOrderByNombreAsc());
        return "banco/nuevo";
    }

    @GetMapping("/bancos/{id}")
    public String detalle(@PathVariable UUID id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        BancoResponse banco = bancoService.obtenerPorId(id);
        List<ResenaResponse> resenas = resenaService.obtenerPorBanco(id);

        boolean esFavorito = userDetails != null
                && usuarioService.esFavorito(id, userDetails.getUsername());

        model.addAttribute("banco", banco);
        model.addAttribute("resenas", resenas);
        model.addAttribute("esFavorito", esFavorito);
        return "banco/detalle";
    }
}
