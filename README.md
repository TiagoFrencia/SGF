# SGF

Repositorio principal del **Sistema de Gestion Farmaceutica**.

Este README describe el estado real auditado del proyecto y su forma actual de ejecucion. Para vision estrategica usar [ROADMAP.md](/C:/Users/tiago/Desktop/SGF/ROADMAP.md). Para estado operativo real usar [MASTER_PLAN.md](/C:/Users/tiago/Desktop/SGF/MASTER_PLAN.md).

## Que es hoy

SGF esta implementado como un monolito modular Spring Boot en `apps/api`, acompanado por un frontend Angular 17 en `apps/web-admin`.

### Implementado

- Catalogo de productos
- Inventario con lotes, vencimientos, transferencias y alertas
- POS y flujo de ventas
- Auditoria
- Sync offline base
- Integraciones base de AFIP, ANMAT y ADESFA
- Catalogo/Vademecum publico CNPM/MSal para prototipo con SNOMED, troquel, GTIN/barcode y precios versionados
- OCR de recetas con Tesseract
- Forecasting y deteccion de fraude
- GraphQL, Swagger, Prometheus y manifests K8s base

### Parcial / Mock / Base

- PAMI SIAFAR: contratos y mock, sin cliente productivo cerrado
- REFEPS: interfaz y mock, sin integracion real cerrada
- ETL legacy: extraccion, transformacion, validacion y dashboard presentes; carga final general aun incompleta
- AlfaBeta/Kairos: adapters/conectores preparados como ruta paga/futura, sin operacion productiva validada
- ONNX: loader y fallback presentes; modelo no empaquetado en recursos
- Interoperabilidad sanitaria real, ETL legacy `load` final y ONNX productivo siguen siendo los frentes principales no cerrados

## Estado tecnico actual

El repo avanzo fuerte en baseline ejecutable y core visible durante esta etapa:

- Testcontainers en Windows + Docker Desktop ya quedo resuelto para `integrationTest`
- Flyway y schema fueron alineados en el tramo critico `V10` a `V21`
- `products` y `sales` ya fueron ajustadas al modelo actual
- JWT y `@ConfigurationProperties` ya quedaron cableadas
- `:sgf-app:test`, `:sgf-app:integrationTest`, `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-ai:test` ya quedaron verdes
- `:sgf-catalog:test`, `:sgf-integrations:test` y `:sgf-app:test` validan el tramo CNPM/MSal y catalogo ampliado
- `apps/web-admin` ya compila y sus tests base ya pasan
- productos, inventario y POS del frontend ya quedaron reorientados a endpoints reales
- ya existe cobertura E2E nueva para catalogo, inventario, POS orders, transferencias y auditoria
- ya existe carga real especifica de Vademecum CNPM/MSal para desarrollo/prototipo, separada de la carga ETL legacy general

El bloqueo real actual ya no es Docker, Flyway, wiring Spring base ni el contrato minimo frontend/backend del core. Los frentes siguientes pasan por profundizar el core visible y cerrar gaps productivos reales como PAMI, REFEPS, ETL legacy `load` y ONNX.

La carga publica CNPM/MSal no reemplaza AlfaBeta/Kairos para venta comercial con SLA; queda documentada como fuente oficial publica apta para prototipo y desarrollo.

## Estructura

- `apps/api`: backend Spring Boot multi-modulo
- `apps/web-admin`: frontend Angular
- `infra/docker`: stack local de desarrollo
- `infra/k8s`: despliegue base en Kubernetes
- `infra/monitoring`: monitoreo
- `Archive`: documentacion historica

## Flujo funcional principal

1. `POST /auth/login`
2. `POST /products`
3. `POST /inventory/receipts`
4. `GET /inventory/stock`
5. `POST /sales`
6. `GET /audit/events`

## Endpoints ya presentes en el repo

### Operacion core

- `POST /auth/login`
- `POST /products`
- `GET /products`
- `GET /products/{productId}`
- `GET /products/search/gtin/{gtin}`
- `POST /inventory/receipts`
- `GET /inventory/stock`
- `GET /inventory/products/{productId}/batches`
- `GET /inventory/alerts/expiry`
- `GET /inventory/alerts/reorder`
- `POST /inventory/transfers`
- `PATCH /inventory/transfers/{id}/ship`
- `PATCH /inventory/transfers/{id}/receive`
- `PATCH /inventory/transfers/{id}/cancel`
- `GET /inventory/transfers`
- `GET /inventory/transfers/{id}`
- `POST /sales`
- `GET /pos/orders`
- `POST /pos/orders`
- `POST /pos/orders/{orderId}/scan`
- `PATCH /pos/orders/{orderId}/ready`
- `POST /pos/orders/{orderId}/complete`
- `GET /pos/orders/{orderId}`
- `GET /audit/events`

### AFIP

- `POST /afip/invoices/sales/{saleId}/authorize`
- `GET /afip/invoices/health`

### ANMAT

- `POST /anmat/datamatrix/parse`
- `GET /anmat/health`
- `POST /anmat/events`
- `GET /anmat/events`
- `GET /anmat/events/by-gtin`
- `GET /anmat/events/by-lot`
- `GET /anmat/serial-summary`
- `GET /anmat/inconsistencies`
- `GET /anmat/dashboard`
- `POST /anmat/remediation-cases/sync`
- `GET /anmat/remediation-cases`
- `PATCH /anmat/remediation-cases/{caseId}`

### ADESFA

- `GET /adesfa/health`
- `POST /adesfa/validations/sales/{saleId}`
- `GET /adesfa/validations/{validationId}`
- `GET /adesfa/validations`

### Vademecum

- `POST /vademecum/sync`
- `POST /vademecum/sync/public-msal?seed=ibuprofeno&seed=paracetamol`
- `POST /vademecum/interactions`
- `GET /vademecum/interactions/{gtin1}/{gtin2}`
- `GET /vademecum/alternatives/{gtin}`
- `GET /vademecum/cheapest/{activeIngredient}`
- `GET /vademecum/equivalent/{gtin1}/{gtin2}`

`/vademecum/sync/public-msal` importa desde CNPM/MSal usando semillas de busqueda. Si no se envian semillas, usa un set inicial de drogas frecuentes para poblar catalogo de desarrollo.

### AI

- `POST /api/ai/ocr/prescription`
- endpoints del modulo AI para forecasting/fraude expuestos desde `sgf-ai` y `sgf-app`

### ETL

- `POST /api/etl/migrations`
- `GET /api/etl/migrations`
- `GET /api/etl/migrations/{id}`
- `GET /api/etl/migrations/{id}/failed`
- `POST /api/etl/migrations/{id}/batch`
- `POST /api/etl/migrations/{id}/execute`
- `POST /api/etl/migrations/{id}/pause`
- `POST /api/etl/migrations/{id}/resume`
- `POST /api/etl/migrations/{id}/abort`

Nota: la carga real CNPM/MSal es un flujo especifico de Vademecum/catalogo. La carga ETL general de sistemas legados sigue pendiente de cierre productivo.

## Build local

Se proveen `gradlew` y `gradlew.bat` para ejecutar Gradle desde Docker en este repo.

### Ejemplos

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
docker compose -f infra/docker/docker-compose.yml up --build
```

## Testing local

SGF separa dos rutas de ejecucion para evitar fallas de harness con Testcontainers en Windows:

- `gradlew` / `gradlew.bat`: compilacion, checks livianos y tests sin Docker desde un contenedor Gradle
- `scripts/test-infra.ps1` y `scripts/test-infra.sh`: tests de infraestructura/integracion con Testcontainers en la JVM del host

### Tests sin Docker

```powershell
.\gradlew.bat :sgf-app:test
.\gradlew.bat :sgf-pos:test
.\gradlew.bat :sgf-integrations:test
.\gradlew.bat :sgf-ai:test
```

### Tests con Testcontainers

Prerequisitos:

- Java 21 disponible en `PATH`
- Docker Desktop iniciado y `docker version` respondiendo
- En Windows, Docker Desktop debe exponer `tcp://127.0.0.1:2375`; ese es el contrato oficial para Testcontainers

Ejemplos:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-infra.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-infra.ps1 :sgf-app:testcontainersProbe
powershell -ExecutionPolicy Bypass -File .\scripts\test-infra.ps1 :sgf-app:integrationTest --tests com.sgf.app.infra.DatabaseConnectionTest
```

El script descarga Gradle `8.8` bajo demanda dentro de `.tools/` y reutiliza ese runtime para los siguientes runs.

### Validacion repo-wide oficial

La ruta oficial y reproducible para validar el backend completo hoy es secuencial:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-backend.ps1
```

Eso ejecuta:

1. `:sgf-pos:test`
2. `:sgf-integrations:test`
3. `:sgf-ai:test`
4. `:sgf-app:test`
5. `:sgf-app:integrationTest`

### Convencion actual de suites

- `test`: excluye pruebas marcadas con `@Tag("integration")`
- `integrationTest`: ejecuta pruebas marcadas con `@Tag("integration")`

### Troubleshooting rapido

- Si falla antes de arrancar Spring, validar `docker version` con `DOCKER_HOST=tcp://127.0.0.1:2375`
- Ejecutar primero `:sgf-app:testcontainersProbe`; si falla, el problema es Docker/Testcontainers/Java y no Spring
- Si falla por puertos o descarga de imagenes, reintentar con Docker Desktop estable y conectividad
- Si falla el contexto pero el contenedor levanta, el problema ya es de wiring Spring, schema o configuracion y no del harness

## Credenciales seed

- Usuario: `admin`
- Password: `admin1234`

## Estado auditado de integraciones

### AFIP

**Base lista**

- Sandbox implementado
- Ruta productiva preparada
- Soporte para WSAA, WSFEv1 y firma CMS
- Endpoint de health para revisar conectividad

**Pendiente real**

- Certificados validos en entorno
- Prueba de homologacion real
- Emision productiva validada extremo a extremo

### ANMAT

**Base lista**

- Parser GS1 DataMatrix
- Registro de eventos
- Dashboard y consultas operativas
- Persistencia de diagnostico de integracion

**Pendiente real**

- Cierre operativo completo en entorno productivo

### ADESFA

**Base lista**

- Validacion sobre ventas reales
- Persistencia economica
- Auditoria y outbox

**Pendiente real**

- Endurecimiento operativo y expansion de escenarios reales

### PAMI SIAFAR

**Parcial / Mock / Base**

- Existe contrato de servicio, DTOs y mock funcional
- El flujo POS puede disparar validacion PAMI

**Pendiente real**

- Cliente SOAP productivo
- Wiring completo con datos reales de sucursal y profesional

### Vademecum publico CNPM/MSal

**Base real implementada**

- Provider comun `VademecumProvider`.
- Implementacion `PublicMsalVademecumProvider` contra `https://cnpm.msal.gov.ar/api/vademecum`.
- Adapters `AlfaBetaVademecumProvider` y `KairosVademecumProvider` como camino pago/futuro.
- Persistencia de SNOMED, troquel, GTIN/barcode, laboratorio, fuente y fecha de vigencia.
- Historial de precios en `product_price_snapshots` con precio retail y precio afiliado PAMI referencial.

**Limite operativo**

- Apto para desarrollo/prototipo con fuente publica oficial.
- No garantiza SLA comercial ni actualizacion diaria; para farmacias reales se mantiene la transicion a AlfaBeta/Kairos.

### REFEPS

**Parcial / Mock / Base**

- Existe interfaz y mock funcional
- El flujo POS puede validar matricula profesional

**Pendiente real**

- Integracion real contra servicio externo

## Estado auditado de AI

### Implementado

- OCR con Tesseract para recetas
- Forecasting
- Deteccion de fraude
- Fallback estadistico para serving AI

### Parcial / Base

- Soporte ONNX a nivel codigo
- No se audito un modelo `.onnx` empaquetado en recursos

## Testing observado

### Implementado

- Tests de infraestructura:
  - `AppConfigTest`
  - `DatabaseConnectionTest`
  - `RedisCacheTest`
  - `MigrationIntegrityTest`
- Integraciones de negocio para PAMI y REFEPS en modo mock
- Pruebas E2E iniciales:
  - `FullFlowIT`
  - `EnterpriseIntegrationIT`
  - `CoreOperationsApiIT`

### Nota operativa

El harness ya no es el bloqueo principal. El baseline tecnico del backend ya quedo verde; los siguientes pendientes son profundizacion funcional del core y frentes productivos aun en modo parcial/mock/base.

## Documentacion

- [00_DASHBOARD.md](/C:/Users/tiago/Desktop/SGF/00_DASHBOARD.md): portada ejecutiva del vault
- [MASTER_PLAN.md](/C:/Users/tiago/Desktop/SGF/MASTER_PLAN.md): estado real y proximos pasos
- [ARCHITECTURE.md](/C:/Users/tiago/Desktop/SGF/ARCHITECTURE.md): arquitectura efectiva
- [ROADMAP.md](/C:/Users/tiago/Desktop/SGF/ROADMAP.md): vision futura
- `Archive/`: historico
