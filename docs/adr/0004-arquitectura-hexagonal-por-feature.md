# ADR-0004: Arquitectura hexagonal organizada por feature

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

El monolito estaba organizado por capas técnicas (`controller`, `service`, `repository`, `dto`).
Las entidades JPA se usaban como modelo de dominio, los servicios dependían de Spring Security
para saber quién era el usuario y la lógica de negocio (quién puede editar un banco, cuándo una
reseña pasa a estar reportada) quedaba repartida entre servicios y controladores.

## Decisión

El backend se organiza en paquetes por *feature* de negocio —`banco`, `resena`, `usuario`,
`favorito`, `checkin`, `moderacion`— más un paquete `shared`. Dentro de cada feature:

```
<feature>/
├── domain/            Modelo y reglas de negocio. Java puro: sin Spring, JPA ni Jackson.
├── application/
│   ├── port/in/       Casos de uso que expone la feature (interfaces).
│   ├── port/out/      Lo que la feature necesita del exterior (persistencia, storage…).
│   └── service/       Implementación de los casos de uso.
└── infrastructure/
    ├── web/           Controladores REST, DTOs de entrada/salida y mappers.
    ├── persistence/   Entidades JPA, repositorios Spring Data y adaptadores de los puertos.
    └── …              Otros adaptadores (Firebase Storage, etc.).
```

Reglas de dependencia, verificadas con ArchUnit en cada build:

1. `domain` solo depende de `java.*` y del dominio compartido (`shared.domain`).
2. `application` no depende de `infrastructure` ni de frameworks web/persistencia.
3. Una feature puede usar el `domain` y los puertos (`application.port`) de otra,
   nunca su `infrastructure`.
4. Los controladores solo hablan con puertos de entrada (`port.in`).

## Consecuencias

- El dominio se prueba con tests unitarios rápidos, sin contexto de Spring.
- Cambiar de base de datos o de proveedor de identidad solo afecta a adaptadores.
- Más clases (puertos, adaptadores, mappers); MapStruct elimina la mayor parte del código de mapeo.
- Las relaciones entre features se modelan por identificador (`BancoId`, `UsuarioId`),
  no con asociaciones JPA entre agregados.
