package com.benchify.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee {@code Authorization: Bearer <ID token>}, lo verifica y deja la autenticación en el
 * contexto de seguridad. Sin cabecera la petición sigue como anónima (las reglas de
 * {@link SecurityConfig} deciden); con un token inválido responde 401 directamente.
 *
 * <p>El rol con el que se autoriza no es el custom claim del token sino el que guarda la base de
 * datos ({@link RoleResolver}): así un administrador degradado pierde el acceso en su siguiente
 * petición aunque su token siga vigente.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final IdTokenVerifier verifier;
    private final RoleResolver roleResolver;
    private final ProblemDetailSecurityHandler problemHandler;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public FirebaseAuthenticationFilter(IdTokenVerifier verifier, RoleResolver roleResolver,
            ProblemDetailSecurityHandler problemHandler) {
        this.verifier = verifier;
        this.roleResolver = roleResolver;
        this.problemHandler = problemHandler;
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
            problemHandler.commence(request, response, new BadCredentialsException(e.getMessage(), e));
            return;
        }

        try {
            user = user.withRol(roleResolver.resolve(user));
        } catch (RuntimeException e) {
            // Sin rol fiable no se puede autorizar: mejor un 500 que dejar pasar la petición con el del token
            log.error("Could not resolve the role of user {}", user.uid(), e);
            contextHolder.clearContext();
            problemHandler.write(request, response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Se ha producido un error inesperado");
            return;
        }

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthentication(user));
        contextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
