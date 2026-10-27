package com.benchify.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee {@code Authorization: Bearer <ID token>}, lo verifica y deja la autenticación en el
 * contexto de seguridad. Sin cabecera la petición sigue como anónima (las reglas de
 * {@link SecurityConfig} deciden); con un token inválido responde 401 directamente.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IdTokenVerifier verifier;
    private final AuthenticationEntryPoint entryPoint;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public FirebaseAuthenticationFilter(IdTokenVerifier verifier, AuthenticationEntryPoint entryPoint) {
        this.verifier = verifier;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        AuthenticatedUser user;
        try {
            if (token.isEmpty()) {
                throw new InvalidIdTokenException("Empty bearer token");
            }
            user = verifier.verify(token);
        } catch (InvalidIdTokenException e) {
            contextHolder.clearContext();
            entryPoint.commence(request, response, new BadCredentialsException(e.getMessage(), e));
            return;
        }

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthentication(user));
        contextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
