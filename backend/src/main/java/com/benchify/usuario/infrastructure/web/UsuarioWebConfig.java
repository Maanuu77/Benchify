package com.benchify.usuario.infrastructure.web;

import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
class UsuarioWebConfig implements WebMvcConfigurer {

    private final SincronizarUsuarioUseCase sincronizarUsuario;
    private final Clock clock;

    UsuarioWebConfig(SincronizarUsuarioUseCase sincronizarUsuario, Clock clock) {
        this.sincronizarUsuario = sincronizarUsuario;
        this.clock = clock;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UsuarioSyncInterceptor(sincronizarUsuario, clock)).addPathPatterns("/api/**");
    }
}
