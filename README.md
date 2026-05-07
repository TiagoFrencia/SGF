# SGF

Monorepo inicial del Sistema de Gestion Farmaceutica.

## Estructura

- `apps/api`: backend Spring Boot modular.
- `apps/web-admin`: cliente web responsive sin build step obligatorio.
- `infra/docker`: stack local con PostgreSQL y app.

## Flujo inicial

1. `POST /auth/login`
2. `POST /products`
3. `POST /inventory/receipts`
4. `GET /inventory/stock`
5. `POST /sales`
6. `GET /audit/events`
7. `POST /afip/invoices/sales/{saleId}/authorize`
8. `POST /anmat/datamatrix/parse`
9. `POST /anmat/events`
10. `GET /anmat/events`
11. `POST /adesfa/validations/sales/{saleId}`

## Build local

Se proveen `gradlew` y `gradlew.bat` que ejecutan Gradle dentro de Docker.

### Ejemplos

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
docker compose -f infra/docker/docker-compose.yml up --build
```

## Credenciales seed

- Usuario: `admin`
- Password: `admin1234`

## AFIP sandbox

La primera iteracion AFIP usa un proveedor desacoplado en modo `SANDBOX`.
Genera CAE simulado y persiste comprobantes fiscales sin conectarse aun a WSAA/WSFEv1 reales.

## AFIP real

Tambien quedo preparada la ruta de integracion real con:

- `WSAA` para obtener `token` y `sign`
- `WSFEv1` para autorizar comprobantes
- firma CMS con certificado X.509 + clave privada

Variables relevantes:

- `APP_AFIP_MODE=PRODUCTION`
- `APP_AFIP_WS_ENVIRONMENT=HOMOLOGATION` o `PRODUCTION`
- `APP_AFIP_CERTIFICATE_PATH=...`
- `APP_AFIP_PRIVATE_KEY_PATH=...`
- `APP_AFIP_PKCS12_PATH=...`
- `APP_AFIP_PKCS12_PASSWORD=...`
- `APP_AFIP_CUIT=...`

Soporta dos estrategias de credenciales:

- par `CRT/KEY` en PEM
- `PKCS12/PFX/P12`

### Primer prueba de homologacion

1. Copiar `infra/docker/.env.example` a `infra/docker/.env`
2. Colocar certificados en `infra/docker/certs`
3. Configurar:
   - `APP_AFIP_MODE=PRODUCTION`
   - `APP_AFIP_WS_ENVIRONMENT=HOMOLOGATION`
   - `APP_AFIP_CUIT=...`
   - `APP_AFIP_CERTIFICATE_PATH` y `APP_AFIP_PRIVATE_KEY_PATH`
   - o `APP_AFIP_PKCS12_PATH` y `APP_AFIP_PKCS12_PASSWORD`
4. Reiniciar:

```powershell
docker compose -f infra/docker/docker-compose.yml up -d --build
```

### Check previo de conectividad

Antes de emitir comprobantes reales, usar:

- `GET /afip/invoices/health`
- `GET /afip/invoices/health?refreshToken=true`

Ese endpoint valida:

- modo y ambiente AFIP activos
- estrategia de certificado cargada
- obtencion de token WSAA en modo real

### Estado actual

- ya se consulta `FECompUltimoAutorizado` para numeracion correlativa
- ya se persisten observaciones, errores, reintentos y ultimo estado fiscal
- ya existe control conservador para no reintentar automaticamente una autorizacion WSFE con riesgo de duplicado

Pendiente para homologacion real:

- disponer de certificados reales AFIP en el entorno
- ejecutar `health?refreshToken=true`
- luego probar una emision real de comprobante sobre una venta local

## ANMAT base

Tambien quedo iniciada la base de trazabilidad ANMAT:

- flag de producto trazable y categoria ANMAT
- parser GS1 `DataMatrix`
- registro de eventos de trazabilidad
- auditoria y outbox para recepcion/dispensa
- adapter con `SANDBOX` y primera ruta `PRODUCTION` via HTTP configurable

Endpoints:

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

Formato soportado inicialmente:

`(01)GTIN(17)VENC(10)LOTE(21)SERIAL`

Validaciones operativas incluidas:

- `DISPENSE` requiere `saleId`
- `DISPENSE` requiere un `RECEIPT` previo del mismo serial
- `DISPENSE` valida que la venta contenga exactamente el producto/lote
- `RETURN` requiere una `DISPENSE` previa del mismo serial

Consultas operativas incluidas:

- resumen de estado actual por serial
- busqueda por GTIN
- busqueda por GTIN + lote
- detector de inconsistencias historicas
- dashboard basico con metricas y alertas recientes

Diagnostico de integracion incluido:

- `GET /anmat/health`
- persistencia de `providerReference`, `integrationMode`, `lastHttpStatus` y `retryable` por evento

## ADESFA base

Tambien quedo iniciada la base de validacion ADESFA:

- validacion de cobertura sobre una venta real
- adapter con `SANDBOX` y primera ruta `PRODUCTION` via HTTP configurable
- persistencia del split economico entre `coverageAmount` y `patientAmount`
- auditoria y outbox por validacion

Endpoints:

- `GET /adesfa/health`
- `POST /adesfa/validations/sales/{saleId}`
- `GET /adesfa/validations/{validationId}`
- `GET /adesfa/validations`

Comportamiento inicial:

- si `validatorCode` no se informa, usa el configurado por defecto
- en `SANDBOX` calcula una cobertura demo del 70% y copago del 30%
- persiste `providerReference`, `integrationMode`, `lastHttpStatus` y `retryable`

Workflow operativo incluido:

- deteccion pura de inconsistencias sin efectos laterales
- sincronizacion explicita de casos de remediacion desde `POST /anmat/remediation-cases/sync`
- estados de caso: `OPEN`, `ACKNOWLEDGED`, `JUSTIFIED`, `RESOLVED`
- filtros por `status`, `assignedTo`, `severity`, `issueCode`, `gtin` y `serialNumber`
- paginacion con `page` y `size`
- ordenamiento con `sortBy` y `sortDirection`
- `JUSTIFIED` y `RESOLVED` requieren `reason`
- reapertura de un caso `RESOLVED -> OPEN` requiere `reason`
- notas, asignacion, motivo y cierre sin alterar el historico original
"# SGF" 
"# SGF" 
