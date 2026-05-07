# Arquitectura SGF (estado real del repositorio)

## Panorama general
SGF está organizado como un **monolito modular en Java/Spring Boot** dentro de `apps/api`, más una app frontend separada en `apps/web-admin` (Angular 17) y manifiestos de infraestructura en `infra/k8s`.

- Backend runtime: Java 21 + Spring Boot 3.3.
- Persistencia principal: PostgreSQL + Flyway.
- Integraciones externas: AFIP, ANMAT, ADESFA y componentes FHIR.
- Capacidades adicionales: sync offline (SQLite), GraphQL, métricas Prometheus, módulo AI.

## Estructura del repositorio
- `apps/api`: backend Gradle multi-módulo.
- `apps/web-admin`: frontend Angular (`ng serve`, `ng build`).
- `infra/k8s`: despliegues/manifiestos de infraestructura.

## Backend `apps/api`: módulos y responsabilidades
Los módulos declarados en `apps/api/settings.gradle` son:

- `sgf-core`: contratos transversales (`DomainEvent`, excepciones), contexto tenant (`TenantContext`, `TenantFilter`) y utilidades base.
- `sgf-catalog`: dominio y API REST de productos (`/products`).
- `sgf-inventory`: stock/recepciones/transferencias y alertas (`/inventory` y controladores específicos).
- `sgf-pos`: ventas POS (`/sales`, órdenes POS, servicios de pricing/descuentos).
- `sgf-audit`: auditoría de eventos de dominio y consulta de auditoría.
- `sgf-integrations`: adaptadores y endpoints de AFIP/ANMAT/ADESFA, ETL, vademécum, outbox.
- `sgf-sync`: base local SQLite y cola de sincronización para modo offline.
- `sgf-ai`: forecasting, detección de fraude y explicabilidad (`/api/ai`).
- `sgf-app`: aplicación ejecutable (`com.sgf.app.SgfApplication`), wiring Spring Boot y configuración agregadora.

## Grafo de dependencias internas (build real)
Dependencias directas entre módulos según `build.gradle`:

- `sgf-core`: sin dependencias a otros módulos.
- `sgf-catalog` -> `sgf-core`
- `sgf-audit` -> `sgf-core`
- `sgf-inventory` -> `sgf-core`, `sgf-catalog`, `sgf-audit`
- `sgf-pos` -> `sgf-core`, `sgf-catalog`, `sgf-audit`, `sgf-inventory`
- `sgf-integrations` -> `sgf-core`, `sgf-catalog`, `sgf-audit`, `sgf-pos`, `sgf-inventory`
- `sgf-sync` -> `sgf-core`, `sgf-pos`, `sgf-inventory`
- `sgf-ai` -> `sgf-core`, `sgf-catalog`, `sgf-inventory`
- `sgf-app` -> todos los módulos anteriores

## Comunicación entre módulos
Se usan dos estrategias que conviven:

1. **Acoplamiento por dependencia de código** (vía Gradle project dependencies), principalmente en servicios síncronos.
2. **Eventos de dominio intra-proceso** (`Spring ApplicationEventPublisher` + `@EventListener`):
   - Eventos en `sgf-core` (ej. `SaleCompletedEvent`, `StockUpdatedEvent`, `ProductCreatedEvent`, `MigrationStartedEvent`).
   - `sgf-audit` escucha eventos para persistir auditoría (`AuditEventListener`).
   - `sgf-integrations` escucha eventos para encolar outbox (`OutboxEventListener`).

## Interfaces expuestas
- REST controllers distribuidos por módulo (`catalog`, `inventory`, `pos`, `audit`, `integrations`, `ai`).
- GraphQL habilitado en `sgf-app` con schema en `sgf-app/src/main/resources/graphql/schema.graphqls` y resolver inicial (`ProductQueryResolver`).
- OpenAPI/Swagger activo vía `springdoc`.

## Datos y consistencia
- Migraciones Flyway centralizadas en `sgf-app/src/main/resources/db/migration`.
- Outbox implementado desde `sgf-integrations` para propagación a integraciones externas.
- `sgf-sync` mantiene almacenamiento SQLite local (`local_products`, `local_sales`, `local_sync_queue`) para operación offline-first.

## Seguridad, tenancy y observabilidad
- Seguridad con Spring Security en `sgf-app` (`SecurityFilterChain`, JWT, method security con `@PreAuthorize`).
- Multi-tenancy en progreso con `TenantFilter`/`TenantContext` en `sgf-core`.
- Métricas de negocio con Micrometer + Prometheus en `sgf-app` (`MetricsConfig`).
- Correlation ID/filtering presente en componentes de app/core para trazabilidad.

## Estado de transición actual
La base muestra una **migración en curso** desde el layout legacy (`com.sgf.modules.*`) al layout modular por bounded context (`com.sgf.catalog`, `com.sgf.inventory`, etc.).  
Hoy conviven ambos namespaces dentro de `sgf-app`, por lo que la arquitectura efectiva es modular pero en refactor activo.
