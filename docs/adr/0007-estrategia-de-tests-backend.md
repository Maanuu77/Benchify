# ADR-0007: Estrategia de tests del backend

- **Estado:** Aceptado
- **Fecha:** 2026-10-26

## Contexto

El monolito tenía un único test (`contextLoads`). La reescritura hexagonal separa dominio,
casos de uso y adaptadores, y cada capa se puede probar a un coste distinto. Además, PostGIS
y Firebase no se pueden sustituir por H2 o por mocks sin dejar de probar lo importante.

## Decisión

| Nivel | Qué cubre | Herramientas |
|-------|-----------|--------------|
| Dominio | Reglas e invariantes de agregados y value objects | JUnit 5, AssertJ |
| Aplicación | Casos de uso con puertos simulados | Mockito |
| Web (slice) | Contrato HTTP, validación, errores RFC 9457 y reglas de seguridad reales | `@WebMvcTest` + `@ApiControllerTest` |
| Persistencia | Adaptadores contra PostgreSQL + PostGIS reales (consultas espaciales, Flyway) | Testcontainers |
| Flujos de API | Recorridos completos entre features | `@SpringBootTest` + MockMvc |
| Arquitectura | Reglas de dependencia del ADR-0004 | ArchUnit |

- **Firebase** se sustituye en los tests por dobles en `TestAdaptersConfig`: un verificador que
  acepta tokens `test:<uid>[:ROL]`, un proveedor de identidad que registra los roles y un
  almacenamiento de imágenes en memoria. Los adaptadores reales de Firebase tienen tests
  unitarios con el SDK simulado y se han validado manualmente contra Firebase Emulator Suite.
- **Base de datos**: un único contenedor `postgis/postgis` compartido. Sin Docker, la variable
  `IT_DATABASE_URL` apunta a un PostGIS existente; sin ninguna de las dos, los tests de
  integración se omiten en lugar de fallar.
- **Cobertura** con JaCoCo, excluyendo el código generado por MapStruct: mínimo de 70 % de
  líneas y 60 % de ramas en cualquier entorno (`jacoco.minimum.line` y
  `jacoco.minimum.branch`). CI ejecuta siempre los tests de integración y exige un umbral mayor.

## Consecuencias

- Los tests de dominio y aplicación corren en milisegundos y no necesitan Spring.
- Las consultas PostGIS y las migraciones se prueban contra el motor real.
- Un portátil sin Docker sigue pudiendo compilar y pasar la suite, aunque con menos cobertura.
