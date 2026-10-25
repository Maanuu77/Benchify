package com.benchify.controller;

import com.benchify.repository.UsuarioRepository;
import com.benchify.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/usuarios/{id}")
    public String perfilPublico(@PathVariable Long id, Model model) {
        model.addAttribute("perfil", usuarioService.obtenerPerfil(id));
        return "usuario/perfil";
    }

    @GetMapping("/mi-perfil")
    public String miPerfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long id = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
        model.addAttribute("perfil", usuarioService.obtenerPerfil(id));
        model.addAttribute("esPropio", true);
        return "usuario/perfil";
    }
}
