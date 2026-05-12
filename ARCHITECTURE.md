# Arquitectura SGF

Estado real del repositorio auditado al `2026-05-12`.

## Panorama general

SGF esta implementado hoy como un **monolito modular en Java/Spring Boot** dentro de `apps/api`, con un frontend **Angular 17** en `apps/web-admin` y manifests de infraestructura en `infra/k8s`.

- Backend runtime: Java 21 + Spring Boot 3.3
- Persistencia principal: PostgreSQL + Flyway
- Cache y soporte operacional: Redis 7+
- Integraciones externas activas o iniciadas: AFIP, ANMAT, ADESFA, Vademecum CNPM/MSal, FHIR, PAMI y REFEPS
- Capacidades complementarias: sync offline, GraphQL, Prometheus/Grafana, AI

## Estructura del repo

- `apps/api`: backend Gradle multi-modulo
- `apps/web-admin`: frontend Angular
- `infra/docker`: stack local
- `infra/k8s`: manifiestos de despliegue
- `infra/monitoring`: Prometheus y dashboard Grafana
- `graphify-out`: contexto de grafo para exploracion tecnica, no fuente primaria de estado

## Modulos backend

Los modulos declarados en `apps/api/settings.gradle` son:

- `sgf-core`: contratos transversales, eventos de dominio, excepciones, tenant context e interfaces de integracion
- `sgf-catalog`: catalogo de productos y API REST de productos
- `sgf-inventory`: stock, recepciones, transferencias y alertas
- `sgf-pos`: ventas, POS orders, pricing y descuentos por obra social
- `sgf-audit`: auditoria de eventos y consulta de auditoria
- `sgf-integrations`: AFIP, ANMAT, ADESFA, ETL, vademecum, outbox y conectores sanitarios
- `sgf-sync`: base local SQLite y cola de sincronizacion offline
- `sgf-ai`: forecasting, fraude, OCR y soporte AI
- `sgf-app`: aplicacion ejecutable y wiring Spring Boot

## Dependencias internas

Dependencias directas mas relevantes:

- `sgf-catalog` -> `sgf-core`
- `sgf-audit` -> `sgf-core`
- `sgf-inventory` -> `sgf-core`, `sgf-catalog`, `sgf-audit`
- `sgf-pos` -> `sgf-core`, `sgf-catalog`, `sgf-audit`, `sgf-inventory`
- `sgf-integrations` -> `sgf-core`, `sgf-catalog`, `sgf-audit`, `sgf-pos`, `sgf-inventory`
- `sgf-sync` -> `sgf-core`, `sgf-pos`, `sgf-inventory`
- `sgf-ai` -> `sgf-core`, `sgf-catalog`, `sgf-inventory`
- `sgf-app` -> todos los modulos anteriores

## Comunicacion entre modulos

Conviven dos mecanismos principales:

1. **Llamadas sincronas por dependencia de codigo** entre servicios del monolito modular.
2. **Eventos de dominio intra-proceso** con `ApplicationEventPublisher` y listeners:
   - `sgf-audit` persiste auditoria a partir de eventos de dominio.
   - `sgf-integrations` alimenta el outbox para integraciones externas.
   - `sgf-ai` y otros modulos pueden engancharse al flujo de negocio con listeners.

## Superficies expuestas

- REST controllers repartidos por bounded context
- GraphQL en `sgf-app`
- Swagger/OpenAPI via `springdoc`
- Frontend Angular con features de dashboard, inventario, productos, POS, migracion, sync y AI

## Contrato actual de configuracion y testing

### Configuracion

- `sgf-app` usa `@ConfigurationPropertiesScan(basePackages = "com.sgf")`
- Los prefijos auditados como vigentes son:
  - `app.jwt`
  - `app.adesfa`
  - `app.afip`
  - `app.anmat`

La capa de configuracion base ya quedo ordenada y estable para el baseline actual.

### Testing

- `test`: suites sin Docker
- `integrationTest`: suites con Testcontainers
- `scripts/test-infra.ps1` y `scripts/test-infra.sh`: entrada oficial para `integrationTest` desde la JVM del host
- `scripts/verify-backend.ps1` y `scripts/verify-backend.sh`: validacion repo-wide secuencial oficial del backend
- En Windows, el contrato actual es `DOCKER_HOST=tcp://127.0.0.1:2375`

El problema principal ya no es el harness de Docker/Testcontainers ni el arranque base del contexto Spring. Ese baseline ya quedo estabilizado y verde en la ruta secuencial oficial.

## Estado real por subsistema

### Core operativo

**Implementado**

- Productos, inventario, ventas, auditoria y autenticacion
- Transferencias, alertas, POS orders y soporte offline base
- Migraciones Flyway centralizadas en `sgf-app`
- Lecturas minimas de catalogo e inventario para cerrar contratos frontend/backend
- E2E cross-module nuevos para catalogo, inventario, POS orders, transferencias y auditoria
- Frontend Angular reorientado en pantallas core visibles para usar contratos reales de backend
- Catalogo/Vademecum publico CNPM/MSal con SNOMED, troquel, GTIN/barcode, laboratorio, fuente y fecha de vigencia
- Precios versionados para desarrollo/prototipo en `product_price_snapshots`

### Integraciones regulatorias

**AFIP**

- `Implementado` en sandbox y con ruta productiva preparada
- Existe soporte para WSAA, WSFEv1, firma CMS y numeracion correlativa
- `Pendiente real`: certificados validos, homologacion y validacion productiva final

**ANMAT**

- `Implementado` como base solida
- Parser GS1 DataMatrix, persistencia de eventos, dashboard, inconsistencias y remediacion
- `Pendiente real`: validacion operativa productiva completa segun entorno

**ADESFA**

- `Implementado` como base robusta
- Validaciones sobre ventas reales, outbox y persistencia del split economico
- `Pendiente real`: endurecimiento operativo y expansion de escenarios reales

**Vademecum / catalogo farmaceutico**

- `Implementado / Base real` para prototipo con `PublicMsalVademecumProvider`
- `VademecumProvider` separa proveedor publico CNPM/MSal de adapters AlfaBeta/Kairos
- CNPM/MSal alimenta catalogo con nombre comercial, presentacion, laboratorio, SNOMED, troquel, GTIN/barcode, fuente y vigencia
- `product_price_snapshots` versiona precio retail y precio afiliado PAMI referencial
- AlfaBeta/Kairos quedan como ruta paga/futura, no como integraciones productivas validadas
- Separacion conceptual: catalogo comercial, terminologia clinica SNOMED y precios versionados

**PAMI SIAFAR**

- `Parcial / Mock / Base`
- Existen contratos, DTOs y `PamiSiafarMockImpl`
- El flujo POS ya dispara la validacion PAMI cuando corresponde
- No se audito cliente SOAP productivo implementado

**REFEPS**

- `Parcial / Mock / Base`
- Existen interfaz y `RefepsMockImpl`
- El flujo POS ya invoca validacion de matricula profesional
- No se audito integracion real en linea

### ETL y migracion

**Implementado**

- Extractores para FarmaWin, Nixfarma y DBF
- `DataTransformer`
- `DataValidator`
- `MigrationDashboard`
- tracking de runs, fallas, rollback y shadow mode
- carga real especifica CNPM/MSal para catalogo/Vademecum y precios versionados

**Parcial / Mock / Base**

- La etapa `load` general aun esta simulada y no consolida persistencia final del dominio SGF para migraciones legacy

### AI

**Implementado**

- `ForecastingService`
- `FraudDetectionService`
- `AnomalyDetector`
- OCR con `TesseractOcrServiceImpl`
- endpoint `POST /api/ai/ocr/prescription`

**Parcial / Mock / Base**

- `OnnxModelLoader` existe y soporta fallback
- El archivo `models/demand_forecast_v1.onnx` no fue encontrado en recursos al auditar el repo

## Frontend Angular

`apps/web-admin` sigue siendo una UI en evolucion, pero ya no depende solo de mocks en los flujos core principales.

### Ya alineado al backend real

- productos
- inventario / stock
- POS base

### Todavia en consolidacion

- terminales POS multiples
- pantallas consultivas de auditoria y alertas
- superficies secundarias de integraciones/AI/dashboard

## Seguridad, tenancy y observabilidad

### Implementado

- Spring Security con JWT
- method security
- filtros de correlation ID
- base de multi-tenancy con `TenantFilter` y `TenantContext`
- metricas con Micrometer + Prometheus
- manifests K8s con probes, HPA e ingress TLS

### Parcial / Base

- El hardening de plataforma no esta completo
- No se observo cierre integral de politicas de red, `securityContext`, PDBs ni controles de plataforma avanzados

## Estado de transicion tecnica

El repo sigue en transicion entre:

- layout legacy `com.sgf.modules.*`
- bounded contexts modernos `com.sgf.catalog`, `com.sgf.inventory`, `com.sgf.pos`, etc.

La arquitectura efectiva ya es modular, pero conviven namespaces y estilos de organizacion distintos dentro de `sgf-app` y modulos relacionados.

## Nota sobre Graphify

`graphify-out/GRAPH_REPORT.md` puede ayudar a navegar comunidades tecnicas y dependencias conceptuales del repo, pero no debe usarse como evidencia primaria de que una capacidad este completa en terminos funcionales u operativos.
