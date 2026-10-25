package com.benchify.controller;

import com.benchify.dto.AuthResponse;
import com.benchify.dto.LoginRequest;
import com.benchify.dto.RegistroRequest;
import com.benchify.exception.EmailAlreadyExistsException;
import com.benchify.security.JwtFilter;
import com.benchify.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @GetMapping("/login")
    public String loginForm(@ModelAttribute("loginRequest") LoginRequest loginRequest,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("error", error);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                        BindingResult result,
                        HttpServletResponse response,
                        Model model) {
        if (result.hasErrors()) {
            return "auth/login";
        }
        try {
            AuthResponse auth = authService.login(loginRequest);
            setAuthCookie(response, auth.getToken());
            return "redirect:/";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Email o contraseña incorrectos");
            return "auth/login";
        }
    }

    @GetMapping("/registro")
    public String registroForm(@ModelAttribute("registroRequest") RegistroRequest registroRequest) {
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registro(@Valid @ModelAttribute("registroRequest") RegistroRequest registroRequest,
                           BindingResult result,
                           HttpServletResponse response,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/registro";
        }
        try {
            AuthResponse auth = authService.registrar(registroRequest);
            setAuthCookie(response, auth.getToken());
            return "redirect:/";
        } catch (EmailAlreadyExistsException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/registro";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // cámbialo a true en producción (HTTPS)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JwtFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // cámbialo a true en producción
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
