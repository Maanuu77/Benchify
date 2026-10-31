package com.benchify.shared.infrastructure.web;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Documentación OpenAPI 3.1 en {@code /v3/api-docs} y Swagger UI en {@code /swagger-ui.html}.
 * Todas las respuestas de error se documentan con el mismo esquema RFC 9457.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "firebase";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    static {
        // El actor lo resuelve la seguridad, no lo envía el cliente
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentActor.class).addRequestWrapperToIgnore(Actor.class);
    }

    @Bean
    OpenAPI benchifyOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        String version = buildProperties.getIfAvailable() != null ? buildProperties.getObject().getVersion() : "dev";
        return new OpenAPI()
                .info(new Info()
                        .title("Benchify API")
                        .version(version)
                        .description("""
                                API REST del mapa colaborativo de bancos: búsqueda por cercanía, reseñas, \
                                favoritos, check-ins y moderación. Las operaciones protegidas requieren el \
                                ID token de Firebase Authentication en la cabecera `Authorization: Bearer`."""))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("ID token de Firebase Authentication"))
                        .addSchemas(PROBLEM_SCHEMA, problemSchema()));
    }

    /** Añade a cada operación las respuestas de error comunes con el esquema de problema. */
    @Bean
    GlobalOperationCustomizer problemResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
            responses.putIfAbsent("400", problem("Petición no válida (validación o formato)"));
            if (secured) {
                responses.putIfAbsent("401", problem("Falta el ID token o no es válido"));
                responses.putIfAbsent("403", problem("El usuario no tiene permiso"));
            }
            responses.putIfAbsent("404", problem("Recurso no encontrado"));
            if (!handlerMethod.hasMethodAnnotation(GetMapping.class)) {
                responses.putIfAbsent("409", problem("Conflicto con el estado actual"));
                responses.putIfAbsent("422", problem("Regla de negocio incumplida"));
                responses.putIfAbsent("429", problem("Demasiadas escrituras seguidas; la cabecera Retry-After "
                        + "indica cuántos segundos esperar"));
            }
            return operation;
        };
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_MEDIA_TYPE,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> problemSchema() {
        Map<String, Schema> properties = Map.of(
                "type", new StringSchema().example("about:blank"),
                "title", new StringSchema().example("Not Found"),
                "status", new IntegerSchema().example(404),
                "detail", new StringSchema().example("Banco no encontrado"),
                "instance", new StringSchema().example("/api/v1/bancos/00000000-0000-0000-0000-000000000000"),
                "code", new StringSchema().example("BANCO_NOT_FOUND")
                        .description("Identificador estable del error para el cliente"),
                "errors", new ArraySchema()
                        .items(new ObjectSchema()
                                .addProperty("field", new StringSchema())
                                .addProperty("message", new StringSchema()))
                        .description("Solo en errores de validación"));
        return new ObjectSchema().description("RFC 9457 Problem Details").properties(properties);
    }
}
