package com.benchify.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS = {
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    ProblemDetailSecurityHandler problemDetailSecurityHandler(ObjectMapper objectMapper) {
        return new ProblemDetailSecurityHandler(objectMapper);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, IdTokenVerifier idTokenVerifier,
            RoleResolver roleResolver, ProblemDetailSecurityHandler problemHandler) {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API stateless con token en la cabecera Authorization: CSRF no aplica
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .addFilterBefore(new FirebaseAuthenticationFilter(idTokenVerifier, roleResolver, problemHandler),
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Lecturas personales que conviven con rutas públicas
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/usuarios/me", "/api/v1/usuarios/me/**",
                                "/api/v1/bancos/*/favorito", "/api/v1/bancos/*/resenas/mia").authenticated()
                        // Lecturas públicas: mapa, fichas, fotos, reseñas, etiquetas y perfiles; las fotos
                        // pendientes solo se sirven a quien las puede ver (lo decide el caso de uso)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/bancos", "/api/v1/bancos/**", "/api/v1/imagenes/**",
                                "/api/v1/resenas/*",
                                "/api/v1/etiquetas",
                                "/api/v1/usuarios/*", "/api/v1/usuarios/*/bancos",
                                "/api/v1/usuarios/*/resenas", "/api/v1/usuarios/*/estadisticas").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .build();
    }
}
