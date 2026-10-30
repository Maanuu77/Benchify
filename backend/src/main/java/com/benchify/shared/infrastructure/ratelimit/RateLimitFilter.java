package com.benchify.shared.infrastructure.ratelimit;

import com.benchify.shared.infrastructure.security.FirebaseAuthentication;
import com.benchify.shared.infrastructure.security.ProblemDetailSecurityHandler;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Limita la frecuencia de las escrituras en la API para que nadie llene la cola de moderación ni
 * el almacenamiento de fotos. Va detrás de Spring Security, con la autenticación ya resuelta: el
 * cubo de un usuario es el de su UID y el de una petición anónima, el de la IP del cliente (con
 * {@code server.forward-headers-strategy=framework}, el primer salto de {@code X-Forwarded-For}).
 * Al agotarlo responde 429 con {@code Retry-After}.
 *
 * <p>Las subidas de fotos tienen su propio cubo, más estricto, y no gastan del de escrituras.
 * Los cubos viven en la memoria de cada instancia: con varias instancias de Cloud Run el límite
 * efectivo se multiplica por su número.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final String CODE = "DEMASIADAS_PETICIONES";

    private static final Set<String> ESCRITURAS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final PathPattern API = PathPatternParser.defaultInstance.parse("/api/**");
    private static final PathPattern SUBIDA_FOTOS =
            PathPatternParser.defaultInstance.parse("/api/v1/bancos/{bancoId}/imagenes");
    private static final long NANOS_POR_SEGUNDO = TimeUnit.SECONDS.toNanos(1);

    private final Cubos escrituras;
    private final Cubos subidas;
    private final ProblemDetailSecurityHandler problemHandler;

    public RateLimitFilter(RateLimitProperties properties, ProblemDetailSecurityHandler problemHandler) {
        this(properties, problemHandler, TimeMeter.SYSTEM_MILLISECONDS);
    }

    RateLimitFilter(RateLimitProperties properties, ProblemDetailSecurityHandler problemHandler, TimeMeter reloj) {
        this.escrituras = new Cubos(properties.writes(), properties.maxKeys(), reloj);
        this.subidas = new Cubos(properties.uploads(), properties.maxKeys(), reloj);
        this.problemHandler = problemHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ESCRITURAS.contains(request.getMethod()) || !API.matches(ruta(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean subida = HttpMethod.POST.matches(request.getMethod()) && SUBIDA_FOTOS.matches(ruta(request));
        ConsumptionProbe consumo = (subida ? subidas : escrituras).consumir(clave(request));
        if (consumo.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        // Redondeo hacia arriba: con 0 el cliente reintentaría en el acto y volvería a fallar
        long segundos = Math.max(1, (consumo.getNanosToWaitForRefill() + NANOS_POR_SEGUNDO - 1) / NANOS_POR_SEGUNDO);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
        problemHandler.write(request, response, HttpStatus.TOO_MANY_REQUESTS, CODE,
                "Has hecho demasiadas peticiones seguidas; vuelve a intentarlo dentro de " + segundos + " s");
    }

    /** Claves recordadas entre los dos cubos (para comprobar en tests que el mapa está acotado). */
    long clavesRecordadas() {
        return escrituras.tamano() + subidas.tamano();
    }

    private static String clave(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthentication firebase) {
            return "uid:" + firebase.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static PathContainer ruta(HttpServletRequest request) {
        return PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    /**
     * Un cubo por clave. Uno que lleva un periodo entero sin usarse estaría lleno otra vez, así que
     * olvidarlo no cambia nada; además el número de claves está acotado.
     */
    private static final class Cubos {

        private final RateLimitProperties.Limit limite;
        private final TimeMeter reloj;
        private final Cache<String, Bucket> porClave;

        Cubos(RateLimitProperties.Limit limite, int maxClaves, TimeMeter reloj) {
            this.limite = limite;
            this.reloj = reloj;
            this.porClave = Caffeine.newBuilder()
                    .maximumSize(maxClaves)
                    .expireAfterAccess(limite.period())
                    .executor(Runnable::run)
                    .build();
        }

        ConsumptionProbe consumir(String clave) {
            return porClave.get(clave, k -> nuevoCubo()).tryConsumeAndReturnRemaining(1);
        }

        long tamano() {
            porClave.cleanUp();
            return porClave.estimatedSize();
        }

        private Bucket nuevoCubo() {
            return Bucket.builder()
                    .addLimit(limit -> limit.capacity(limite.capacity()).refillGreedy(limite.capacity(), limite.period()))
                    .withCustomTimePrecision(reloj)
                    .build();
        }
    }
}
