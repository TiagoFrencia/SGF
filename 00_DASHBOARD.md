# SGF Dashboard

Portada ejecutiva del repositorio y del vault de Obsidian del **Sistema de Gestion Farmaceutica**.

---

## Estado general

- **Enfoque documental:** auditoria real del repo, no roadmap aspiracional.
- **Progreso documental estimado:** `||||||||||` (90%)
- **Fase operativa dominante:** `Core operativo visible sobre baseline backend ya verde`.
- **Ultima auditoria manual:** `2026-05-12`

### Convencion de estados

- `Implementado`
- `Parcial / Mock / Base`
- `Pendiente real`
- `Historico / Archivado`

---

## Navegacion principal

- [[MASTER_PLAN]]: estado operativo real, bloqueos actuales y proximos pasos verificables.
- [[ARCHITECTURE]]: arquitectura efectiva del repo, integraciones y estado de wiring.
- [[README]]: onboarding tecnico y contrato actual de testing.
- [[ROADMAP]]: vision estrategica a mediano y largo plazo.
- [[Archive/|Archive]]: material historico que no debe leerse como estado actual.

---

## Resumen auditado

### Implementado

- Monolito modular Spring Boot en `apps/api` con frontend Angular 17 en `apps/web-admin`.
- Modulos principales de catalogo, inventario, POS, auditoria, sync, integraciones, AI y app agregadora.
- Integraciones base de AFIP, ANMAT y ADESFA con endpoints, persistencia y tests.
- OCR de recetas con Tesseract y endpoint REST dedicado.
- Forecasting, deteccion de fraude y loader ONNX con fallback.
- Harness host-first para `integrationTest` con `scripts/test-infra.ps1` y `scripts/test-infra.sh`.
- Contrato operativo de Testcontainers resuelto en Windows con `DOCKER_HOST=tcp://127.0.0.1:2375`.
- Correcciones de migraciones y schema desde `V10` hasta `V21`.
- Registro global de `@ConfigurationProperties` ya operativo.
- Duplicidad de `AdesfaGateway` ya resuelta.
- Ciclo de `SyncAutoConfiguration` ya resuelto.
- `MigrationIntegrityTest` y `DatabaseConnectionTest` ya pasan.
- Auditoria multi-tenant ya alineada para `audit_events.tenant_id`.
- `SgfApiApplicationTests` ya quedo verde completo.
- `AppConfigTest`, `EnterpriseIntegrationIT` y `FullFlowIT` ya quedaron verdes.
- `:sgf-app:test` y `:sgf-app:integrationTest` ya quedaron verdes.
- `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-ai:test` ya quedaron verdes.
- `scripts/verify-backend.ps1` y `scripts/verify-backend.sh` ya fijan la validacion repo-wide secuencial del backend.
- `apps/web-admin` ya compila y sus tests base ya quedaron verdes.
- Contratos frontend/backend core ya alineados en productos, inventario y POS.
- Nuevos E2E ya validan catalogo, inventario, POS orders, transferencias y auditoria en un mismo flujo cross-module.
- Catalogo/Vademecum publico CNPM/MSal implementado como base real para prototipo y desarrollo.
- Importador `PublicMsalVademecumProvider` y endpoint `POST /vademecum/sync/public-msal` ya disponibles.
- `V22__public_vademecum_catalog.sql` agrega metadata farmaceutica: SNOMED, troquel, GTIN/barcode, laboratorio, fuente y fecha.
- Precios versionados en `product_price_snapshots` para precio retail y precio afiliado PAMI referencial.

### Parcial / Mock / Base

- PAMI SIAFAR: contratos y `mock`, sin cliente SOAP productivo cerrado.
- REFEPS: interfaz y `mock`, sin validacion productiva en linea.
- ETL legacy: extractores, transformacion, validacion, dashboard y tracking; carga final general todavia simulada.
- AlfaBeta/Kairos: adapters y conectores base presentes, pero no integracion paga/productiva validada.
- ONNX: runtime y loader listos, pero sin modelo empaquetado en recursos.
- Hardening empresarial: manifiestos y probes presentes, pero falta seguridad operativa mas profunda.

### Pendiente real

- Mantener verde la validacion repo-wide secuencial y seguir usando corridas no paralelas como fuente de verdad.
- Profundizar el core operativo visible en terminales POS multiples, transferencias multi-sucursal y auditoria/alertas.
- Elegir el siguiente frente funcional productivo a cerrar: PAMI real, REFEPS real, ETL legacy `load` real u ONNX productivo.
- Ampliar pruebas end-to-end regulatorias y operativas sobre el baseline ya estabilizado.

---

## Alertas criticas

- Docker/Testcontainers ya no es el bloqueo principal; el harness base ya quedo operativo.
- ADESFA y `sgf-sync` ya no son el bloqueo principal del baseline.
- `SgfApiApplicationTests` ya no es el bloqueo principal; ya quedo verde completo.
- La suite amplia de `sgf-app` ya no es el bloqueo principal; `AppConfigTest`, `EnterpriseIntegrationIT`, `FullFlowIT`, `:sgf-app:test` y `:sgf-app:integrationTest` ya quedaron verdes.
- La validacion repo-wide base del backend ya no es el bloqueo principal; `:sgf-pos:test`, `:sgf-integrations:test` y `:sgf-ai:test` tambien quedaron verdes.
- El cierre contractual minimo entre frontend y backend core ya no es el bloqueo principal.
- Vademecum publico CNPM/MSal ya no es pendiente: existe carga real especifica para prototipo, aunque no reemplaza servicios pagos con SLA.
- El siguiente foco real se movio a profundizacion operativa del core y a los frentes productivos aun `Parcial / Mock / Base`, no al baseline tecnico.
- PAMI y REFEPS no deben describirse como integraciones productivas completas: hoy siguen en base contractual mas mocks.
- El reporte de `graphify-out` sirve para navegacion y contexto, no como fuente primaria de verdad funcional.

---

## Guia rapida para agentes

1. Verificar el codigo antes de marcar estados como completados.
2. Tratar `MASTER_PLAN.md` como documento operativo actual.
3. Tratar `ROADMAP.md` como vision futura, no como evidencia de implementacion.
4. Usar `README.md` como contrato actual de ejecucion local y testing.
5. No mover ni limpiar archivos historicos fuera del alcance documental acordado.
