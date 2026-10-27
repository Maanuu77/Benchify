# ADR-0003: Mantener Spring Boot 4.x en lugar de volver a 3.x

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

El plan de modernización pedía "la última Spring Boot 3.x estable". Al auditar el código,
el proyecto ya usaba Spring Boot 4.0.6. La rama 3.5 —la última de 3.x— terminó su soporte
OSS en junio de 2026; seguir en ella supondría arrancar un proyecto nuevo sin parches de
seguridad gratuitos.

Spring Boot 4 trae Spring Framework 7, Spring Security 7, Hibernate 7, Jackson 3,
Testcontainers 2 y autoconfiguración modular (`spring-boot-starter-flyway`,
`spring-boot-starter-webmvc`, starters de test por tecnología).

## Decisión

Usar la última versión estable de Spring Boot 4 (4.1.2 en la fecha de este ADR) con Java 21 LTS.
La decisión se confirmó con el propietario del producto antes de empezar.

## Consecuencias

- Soporte y parches vigentes para el ciclo de vida del portfolio.
- Algunas APIs cambian respecto a la documentación más extendida de Boot 3:
  `@MockitoBean` en lugar de `@MockBean`, paquetes de Jackson 3 (`tools.jackson`),
  Testcontainers 2 (`org.testcontainers.postgresql`), starters de test separados.
- springdoc-openapi debe estar en su línea 3.x, compatible con Boot 4.
