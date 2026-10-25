package com.benchify.controller;

import com.benchify.service.ModeracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERADOR','ADMIN')")
public class AdminWebController {

    private final ModeracionService moderacionService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("estadisticas", moderacionService.obtenerEstadisticas());
        return "admin/dashboard";
    }

    @GetMapping("/bancos")
    public String moderacionBancos(Model model) {
        model.addAttribute("bancos", moderacionService.obtenerBancosPendientes());
        return "admin/moderacion-bancos";
    }

    @GetMapping("/resenas")
    public String moderacionResenas(Model model) {
        model.addAttribute("resenas", moderacionService.obtenerResenasPendientesModeracion());
        return "admin/moderacion-resenas";
    }
}
