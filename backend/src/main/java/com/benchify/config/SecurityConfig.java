package com.benchify.config;

import com.benchify.security.JwtFilter;
import com.benchify.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT stateless: nada de sesiones HTTP
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // API stateless con token en la cabecera Authorization: CSRF no aplica
            .csrf(csrf -> csrf.disable())
            // Usa el CorsConfigurationSource definido por perfil (CorsConfig)
            .cors(Customizer.withDefaults())

            // La consola H2 carga en un <iframe>; permitir same-origin para que se vea.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()))

            .authorizeHttpRequests(auth -> auth
                // H2 console (solo en local — desactivar en producción)
                .requestMatchers("/h2-console/**").permitAll()
                // API pública (mapa, fichas, perfiles)
                .requestMatchers(HttpMethod.GET, "/api/bancos/**", "/api/etiquetas/**", "/api/usuarios/**").permitAll()
                // Auth (login, registro, logout)
                .requestMatchers("/api/auth/**").permitAll()
                // Errores
                .requestMatchers("/error").permitAll()
                // Panel de admin
                .requestMatchers("/api/admin/**").hasAnyRole("MODERADOR", "ADMIN")
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"No autenticado\"}");
                }))

            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
