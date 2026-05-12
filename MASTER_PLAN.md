# SGF Master Plan

Documento operativo basado en auditoria real del repositorio al `2026-05-12`.

Este archivo describe el estado actual del proyecto y los proximos pasos verificables. No reemplaza la vision estrategica de [[ROADMAP]].

---

## Resumen ejecutivo

- **Estado general del repo:** baseline tecnico del backend ya verde, core operativo visible avanzado, fuente publica real CNPM/MSal incorporada y cierre vertical POS -> venta -> stock -> auditoria -> outbox reforzado.
- **Progreso global estimado:** `96%`
- **Modelo documental:** `Implementado` / `Parcial / Mock / Base` / `Pendiente real`
- **Fase con mayor traccion actual:** cierre del core operativo visible y profundizacion funcional sobre baseline ya estabilizado.

## Dashboard de progreso

| Frente | Descripcion | Estado | Progreso |
| :--- | :--- | :--- | :--- |
| **Baseline ejecutable backend** | Harness, migraciones, contexto Spring e integracion critica | Implementado | 100% |
| **Core operativo** | Catalogo, inventario, POS, auditoria, sync base y Vademecum publico CNPM/MSal | Implementado | 100% |
| **Integraciones regulatorias** | AFIP, ANMAT, ADESFA, outbox enriquecido y disparo configurable | Parcial / Base | 83% |
| **ETL productivo** | Extraccion, transformacion, validacion, tracking y carga especifica CNPM/MSal | Parcial / Base | 65% |
| **Interoperabilidad sanitaria real** | PAMI SIAFAR, REFEPS y wiring sanitario productivo | Parcial / Mock | 35% |
| **AI productiva** | OCR, forecasting, fraude y ONNX | Parcial / Base | 68% |
| **Hardening enterprise** | K8s, seguridad, resiliencia y readiness comercial | Parcial / Base | 42% |

---

## Estado por frente

### 1. Baseline ejecutable backend

**Estado:** `Implementado`

#### Implementado

- Estrategia host-first para Testcontainers en Windows con `scripts/test-infra.ps1` y `scripts/test-infra.sh`.
- Contrato operativo para `integrationTest` con `DOCKER_HOST=tcp://127.0.0.1:2375`.
- `testcontainersProbe` operativo y alineacion de dependencias Testcontainers.
- Correcciones de migraciones y baseline Flyway desde `V10` hasta `V21`.
- Alineacion de schema con el modelo actual en `products`, `sales`, POS orders y transfers.
- Registro global de `@ConfigurationProperties` mediante `@ConfigurationPropertiesScan(basePackages = "com.sgf")`.
- JWT y profile `test` ya alineados con `app.jwt.*`.
- Duplicidad de `AdesfaGateway` ya resuelta con una sola implementacion activa.
- Ciclo de `SyncAutoConfiguration` ya resuelto separando scheduling del registro de beans.
- `MigrationIntegrityTest` y `DatabaseConnectionTest` ya pasan en el harness host-first.
- Auditoria multi-tenant ya alineada para `audit_events.tenant_id`.
- `SgfApiApplicationTests` ya quedo verde completo.
- `AppConfigTest`, `EnterpriseIntegrationIT` y `FullFlowIT` ya quedaron verdes.
- `:sgf-app:test` y `:sgf-app:integrationTest` ya quedaron verdes en corrida secuencial host-first.
- `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-ai:test` ya quedaron verdes.
- `scripts/verify-backend.ps1` y `scripts/verify-backend.sh` ya fijan una ruta oficial de validacion secuencial del backend.
- El scheduling global ya fue desacoplado del wiring de beans mediante `app.background-jobs.enabled`.
- Los contenedores de prueba ya quedaron estabilizados para la suite completa con soporte singleton de Postgres/Redis.
- Endpoints de auditoria e integraciones regulatorias ya corregidos para no depender del flag `-parameters`.
- Parser OCR de recetas ya corregido para formatos `Formulario Nro` / `Receta Nro`.
- Tests de recibo ya alineados con el formato real del ticket.

#### Pendiente real

- Mantener verde la validacion repo-wide secuencial con `scripts/verify-backend.ps1` y `scripts/verify-backend.sh`.
- Evitar interpretar fallos transitorios de filesystem o corridas paralelas como regresiones del producto.

### 2. Core operativo

**Estado:** `Implementado / Muy avanzado`

#### Implementado

- Catalogo de productos.
- Catalogo/Vademecum publico CNPM/MSal como base real de prototipo para medicamentos comercializados.
- Metadata farmaceutica persistida: SNOMED, troquel, GTIN/barcode, laboratorio, fuente y fecha de vigencia.
- Precios versionados en `product_price_snapshots` para precio retail y precio afiliado PAMI referencial.
- Inventario con lotes, vencimientos, transferencias y alertas.
- POS, ventas, ordenes POS y descuentos por obra social.
- Auditoria y outbox.
- Sync offline con base local y cola de sincronizacion.
- Frontend Angular con dashboard, inventario, productos, POS, migracion y AI.
- Servicios Angular core ya reorientados al backend real para productos, inventario, transferencias, POS orders y auditoria.
- Lecturas minimas ya expuestas para cerrar contratos frontend/backend:
  - `GET /products`
  - `GET /products/{productId}`
  - `GET /products/search/gtin/{gtin}`
  - `GET /inventory/products/{productId}/batches`
- Pantallas visibles de productos, stock y POS ya quedaron funcionales contra contratos reales.
- `apps/web-admin` ya compila y sus tests base ya quedaron verdes.
- E2E cross-module nuevos en `CoreOperationsApiIT` ya validan:
  - crear producto
  - recibir stock
  - consultar stock y alertas
  - crear draft POS
  - scan add
  - mark ready
  - complete order
  - transferencias create/ship/receive y rechazo de transiciones invalidas
  - consulta de auditoria
- Dashboard ya fue reconvertido para operar sobre contratos reales:
  - `GET /inventory/alerts/reorder`
  - `GET /inventory/alerts/expiry`
  - `GET /audit/events`
  - health basico de AFIP, ANMAT y ADESFA
- Ruta visible dedicada `/audit` ya implementada en `apps/web-admin`.
- Pantalla consultiva de auditoria ya muestra:
  - eventos recientes con `limit`
  - verificacion de cadena via `GET /audit/events/verify`
- Inventario visible ya cubre transferencias multi-sucursal con flujo operativo:
  - create
  - ship
  - receive
  - cancel
  - listado por sucursal y estado
- POS visible ya soporta terminales multiples:
  - nueva orden por terminal
  - listado de ordenes abiertas
  - cambio de orden activa
  - recovery de terminal
  - cierre de terminal
  - remocion de orden de terminal
- POS visible ya fue endurecido para operacion multi-cajero:
  - cola operativa por terminal separada en `DRAFT` y `READY`
  - contadores por estado en terminal activa
  - refresh deterministico de terminal luego de completar una orden
- Tests Angular minimos nuevos ya cubren:
  - dashboard
  - auditoria
  - inventory transfer flow
  - POS terminal mode
- `CoreOperationsApiIT` ya amplia cobertura sobre:
  - terminal switching y recovery
  - invalid switch
  - transferencias disputadas
  - filtros por sucursal/estado
  - `GET /audit/events/verify`
- Inventario visible ya fue endurecido como consola operativa de transferencias:
  - filtros por vista, estado y producto
  - recepcion con cantidad explicita para disparar disputa
  - resumen visible por estado `PENDING / IN_TRANSIT / DISPUTED / RECEIVED`
- Dashboard y `/audit` ya fueron endurecidos como capa consultiva operativa:
  - navegacion contextual desde alertas hacia inventario
  - navegacion contextual desde actividad reciente hacia auditoria
  - filtros cliente-side por tipo de evento y agregado
  - estado de verificacion de cadena visible y persistente en la vista
- Importador publico `PublicMsalVademecumProvider` y endpoint `POST /vademecum/sync/public-msal` ya disponibles para poblar catalogo de desarrollo.
- Servicio de precios de catalogo ya resuelve el ultimo snapshot vigente de CNPM/MSal por producto.
- `ProductResponse` ya expone precio retail vigente, precio afiliado PAMI referencial, descuento PAMI, fuente y fecha efectiva.
- POS ya permite scan/add por GTIN con precio automatico desde catalogo cuando no se ingresa precio manual.
- POS mantiene override manual de precio para operacion de caja y casos especiales.
- Respuestas POS ya exponen GTIN, troquel, fuente y fecha de fuente del producto en cada item.
- Venta completada ya publica `SaleCompletedEvent` enriquecido con metodo de pago, documento, datos PAMI/medico e items con GTIN, troquel, lote, precio y trazabilidad.
- Validacion PAMI en ventas ya usa identificador comercial por prioridad `troquel -> GTIN -> productId`.
- Tests `:sgf-catalog:test`, `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-app:test` quedaron verdes el `2026-05-12` con el cierre CNPM/POS y venta/outbox.

#### Pendiente real

- Si se decide seguir en core visible, la siguiente capa ya no es contractual sino de operacion extendida:
  - multi-cajero por sucursal con mayor densidad de trabajo simultaneo
  - manejo de excepciones de transferencias multi-sucursal mas rico
  - auditoria y alertas como consola diaria mas profunda

### 3. Integraciones regulatorias

**Estado:** `Parcial / Base`

#### Implementado

- AFIP con soporte sandbox y ruta productiva preparada.
- ANMAT con parser DataMatrix, eventos, dashboard e inconsistencias.
- ADESFA con validacion sobre ventas reales, persistencia y outbox.
- Auditoria regulatoria e integracion con outbox.
- Vademecum unificado mediante interfaz `VademecumProvider`, con implementacion publica CNPM/MSal y adapters AlfaBeta/Kairos preparados como ruta paga/futura.
- Outbox de `SALE_COMPLETED` ya transporta payload operativo enriquecido para integraciones regulatorias.
- `OutboxProcessor` ya procesa ventas completadas con derivacion AFIP configurable mediante `app.afip.auto-invoice-enabled`.
- ANMAT no bloquea la caja si faltan serial/lote suficiente; los productos trazables vendidos sin datos completos quedan visibles como caso a remediar/loguear antes de reporte productivo.

#### Parcial / Mock / Base

- AFIP real requiere certificados y homologacion efectiva.
- ANMAT productivo depende de configuracion externa y validacion operativa final.
- ADESFA ya no esta bloqueada por properties ni wiring base, pero aun requiere cierre operativo y mas cobertura funcional.
- AlfaBeta/Kairos no deben tratarse como integraciones productivas completas: hay conectores/adapters, falta contrato pago, credenciales, SLA y validacion operativa.

#### Pendiente real

- Aumentar pruebas end-to-end desde venta hasta integracion regulatoria con escenarios mas cercanos a homologacion real, especialmente certificados AFIP y trazabilidad ANMAT con DataMatrix real.

### 4. ETL y migracion de legados

**Estado:** `Parcial / Base`

#### Implementado

- Extractores para FarmaWin, Nixfarma y DBF generico.
- `DataTransformer` para normalizacion y mapeo de datos legacy.
- `DataValidator` para controles estructurales, farmaceuticos y de negocio.
- `MigrationDashboard`, controller ETL, tracking y manejo de fallas.
- Base de rollback y shadow mode.
- Carga real especifica para Vademecum publico CNPM/MSal hacia catalogo SGF y snapshots de precio.

#### Parcial / Mock / Base

- La etapa `load` general para migracion de legados todavia no persiste todo el dominio SGF de forma real; hoy sigue simulada fuera del caso especifico CNPM/MSal.

#### Pendiente real

- Implementar carga real de entidades SGF en la etapa `load`.
- Completar documentacion operativa de migracion y rollback.

### 5. Interoperabilidad sanitaria real

**Estado:** `Parcial / Mock`

#### Implementado

- Contratos de PAMI SIAFAR y REFEPS en `sgf-core`.
- Integracion de ventas que invoca validacion PAMI y validacion profesional.
- Ventas PAMI ya mapean items con identificador comercial correcto por prioridad `troquel -> GTIN -> productId`.
- Mocks funcionales para PAMI SIAFAR y REFEPS.
- Base FHIR y componentes vinculados a interoperabilidad.
- Codigos SNOMED de medicamentos ya se almacenan desde CNPM/MSal como terminologia clinica de referencia para desarrollo.

#### Parcial / Mock / Base

- PAMI SIAFAR sigue en modo `mock`; no se encontro cliente SOAP productivo implementado.
- REFEPS sigue en modo `mock`; no se encontro cliente real contra servicio externo.
- El flujo de ventas aun contiene valores base para datos reales de sucursal y medico; falta configuracion productiva por farmacia/profesional.
- SNOMED en catalogo no equivale a interoperabilidad sanitaria productiva completa; es una base terminologica.

#### Pendiente real

- Implementar cliente real de PAMI SIAFAR.
- Implementar validacion real de REFEPS.
- Cerrar wiring productivo de recetas sanitarias en el flujo POS.

### 6. AI operativa y serving productivo

**Estado:** `Parcial / Base`

#### Implementado

- `ForecastingService`
- `FraudDetectionService`
- `AnomalyDetector`
- OCR de recetas con `TesseractOcrServiceImpl`
- Endpoint `POST /api/ai/ocr/prescription`
- `OnnxModelLoader` con fallback a modelos estadisticos

#### Parcial / Mock / Base

- El soporte ONNX existe a nivel runtime, pero no se encontro `models/demand_forecast_v1.onnx` en recursos.
- El OCR existe y es usable, pero sigue siendo MVP y requiere hardening operativo.

#### Pendiente real

- Empaquetar y validar modelo ONNX real.
- Ampliar pruebas del flujo OCR con archivos reales.

### 7. Hardening empresarial y preparacion comercial

**Estado:** `Parcial / Base`

#### Implementado

- Manifests K8s para API, frontend, Postgres, Redis e ingress.
- Probes, HPA y scraping Prometheus.
- Dashboard Grafana y script de load test base.

#### Parcial / Mock / Base

- No se evidencio hardening completo de seguridad en K8s:
  - `NetworkPolicy`
  - `PodDisruptionBudget`
  - `securityContext`
  - cuotas y limites de plataforma mas finos

#### Pendiente real

- Completar hardening de plataforma y seguridad operativa.
- Expandir testing de carga y resiliencia.

---

## Bloqueo principal actual

El proyecto ya no esta frenado por Docker/Testcontainers, por el tramo grueso de Flyway, por ADESFA, por el ciclo de `sgf-sync`, por la auditoria multi-tenant en login, por la validacion repo-wide base del backend, ni por el contrato minimo frontend/backend del core visible.

- `MigrationIntegrityTest` y `DatabaseConnectionTest` ya quedaron verdes
- `SgfApiApplicationTests` ya quedo verde completo
- `AppConfigTest`, `EnterpriseIntegrationIT` y `FullFlowIT` ya quedaron verdes
- `:sgf-app:test` y `:sgf-app:integrationTest` ya quedaron verdes
- `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-ai:test` ya quedaron verdes
- `:sgf-catalog:test`, `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-app:test` quedaron verdes tras el cierre vertical venta/regulatorio y CNPM/POS
- `scripts/verify-backend.ps1` y `scripts/verify-backend.sh` ya fijan una ruta oficial de validacion secuencial del backend
- `apps/web-admin` ya compila y `npm test -- --watch=false --browsers=ChromeHeadless` ya queda verde
- `CoreOperationsApiIT` ya cubre y valida flujos cross-module del core operativo
- POS multi-cajero visible ya quedo endurecido
- transferencias multi-sucursal visibles ya quedaron endurecidas
- dashboard y auditoria visibles ya quedaron endurecidos como capa consultiva basica
- Vademecum publico CNPM/MSal ya quedo integrado como carga real especifica para catalogo y precios versionados de desarrollo
- CNPM/MSal -> catalogo -> precio -> POS ya quedo cerrado como base real de prototipo/desarrollo
- POS -> venta -> stock -> auditoria -> outbox ya quedo cerrado con payload enriquecido y AFIP configurable

El siguiente frente real ya no esta en el baseline tecnico, en el cierre contractual minimo del core ni en el primer cierre CNPM/POS. Queda en dos capas:

1. profundizacion operativa avanzada del core visible ya implementado:
   - multi-cajero por sucursal con mayor densidad de sesiones y colas
   - transferencias multi-sucursal con manejo mas rico de excepciones
   - auditoria y alertas como consola diaria mas profunda
2. gaps productivos todavia `Parcial / Mock / Base`, especialmente PAMI/REFEPS reales, homologacion AFIP/ANMAT con credenciales/datos reales, carga ETL legacy efectiva y serving AI productivo.

## Proximos pasos verificables

1. Mantener verde `scripts/verify-backend.ps1` o `.sh` como baseline minimo obligatorio del backend.
2. Elegir el siguiente frente funcional productivo a cerrar: PAMI real, REFEPS real, homologacion AFIP, trazabilidad ANMAT con DataMatrix real, ETL legacy `load` real u ONNX productivo.
3. Ampliar pruebas end-to-end regulatorias y operativas sobre los modulos ya estabilizados.
4. Si se decide seguir en core visible avanzado, profundizar consola diaria de auditoria/alertas, excepciones de transferencias y multi-cajero de alta densidad.
5. Recien despues avanzar en hardening enterprise y readiness comercial.

---

## Notas de consistencia

- `ROADMAP.md` debe leerse como vision estrategica.
- `00_DASHBOARD.md` debe reflejar el mismo bloqueo principal y la misma fase dominante.
- `ARCHITECTURE.md` debe reflejar el monolito modular actual, el contrato de testing host-first y el estado de consolidacion del wiring.
- `graphify-out/GRAPH_REPORT.md` es apoyo de navegacion tecnica, no prueba funcional.
- Los archivos en `Archive/` se consideran material historico.
