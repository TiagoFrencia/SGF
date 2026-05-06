# FASE 1 — Pendientes

> Última actualización: 2026-05-06

## ⚠️ Tests pendientes de crear/actualizar

### 1. Tests de compilación e integración
- [ ] **Actualizar imports de tests existentes** — Los 3 tests actuales (`SgfApiApplicationTests`, `AfipBuildersTests`, `AnmatTraceabilityTests`) todavía usan imports viejos (`com.sgf.modules.*`, `com.sgf.api.*`) que romperán al compilar con la nueva estructura modular.
- [ ] **Mover tests a sus submódulos correspondientes**:
  - `AfipBuildersTests` → `sgf-integrations/src/test/`
  - `AnmatTraceabilityTests` → `sgf-integrations/src/test/`
  - `SgfApiApplicationTests` → `sgf-app/src/test/` (test de integración e2e)

### 2. Tests unitarios del módulo sgf-core
- [ ] `BaseEntity` — verificar `@PrePersist` genera UUID, `@PreUpdate` actualiza timestamp
- [ ] `EventBus` — publicar evento, verificar que listeners reciben, comportamiento con listener que tira excepción
- [ ] `BadRequestException` / `NotFoundException` / `ConflictException` — constructores y mensaje
- [ ] `DomainEvent` — contrato (record o implementación concreta)

### 3. Tests unitarios del módulo sgf-sync (offline-first) 🔴 Crítico
- [ ] `LocalDatabase` — inicializar DB, verificar que tablas se crean, múltiples conexiones concurrentes
- [ ] `LocalSyncQueue` — enqueue, pendingEntries con limit, markProcessed, markFailed con dead letter después de 5 reintentos
- [ ] `LocalCommandHandler` — handle persiste localmente y encola evento, falla si SQLite no disponible
- [ ] `SyncReplayProcessor` — replay con remote online (procesa todo), replay con remote offline (no procesa nada), forceReplay
- [ ] `RemoteSyncClient` — isOnline true/false, send exitoso, send con error HTTP 4xx/5xx
- [ ] `LastWriteWinsResolver` — local más nuevo gana, remote más nuevo gana, empate, valores nulos

### 4. Tests unitarios del módulo sgf-inventory
- [ ] `InventoryService.receive()` — primer ingreso crea batch, segundo ingreso mismo lote acumula, lote con distinta fecha de vencimiento rechaza
- [ ] `InventoryService.reserve()` — stock suficiente (FEFO), stock insuficiente, múltiples lotes
- [ ] `InventoryService.stock()` — ordenado por vencimiento

### 5. Tests unitarios del módulo sgf-catalog
- [ ] `ProductService.create()` — producto válido, GTIN duplicado, SKU duplicado
- [ ] `ProductService.findByGtin()` — encontrado, no encontrado

### 6. Tests unitarios del módulo sgf-pos
- [ ] `SalesService.create()` — venta válida, idempotencia (misma key devuelve misma venta), stock insuficiente
- [ ] `SalesService` — cálculo correcto de totalAmount y subtotals

### 7. Tests unitarios del módulo sgf-audit
- [ ] `AuditService.record()` — evento de auditoría se persiste correctamente
- [ ] `AuditController` — GET /audit/events retorna lista paginada

### 8. Tests unitarios del módulo sgf-integrations
- [ ] **AFIP**: `AfipSandboxAuthorizationProvider` — CAE generado, datos simulados correctos
- [ ] **AFIP**: `AfipService` — autorizar factura, idempotencia por saleId
- [ ] **ANMAT**: `AnmatDataMatrixParser` — formatos válidos, formatos inválidos, casos borde (GTIN corto, fecha mal formateada)
- [ ] **ANMAT**: `AnmatTraceabilityService` — DISPENSE requiere RECEIPT previo, DISPENSE requiere saleId, RETURN requiere DISPENSE previa
- [ ] **ADESFA**: `AdesfaService` — validación sandbox 70/30 split, health endpoint
- [ ] **Outbox**: `OutboxService` — enqueue persiste evento PENDING, procesamiento de batch

### 9. Tests de integración e2e
- [ ] Flujo completo con Testcontainers: login → producto → stock → venta → AFIP → ANMAT → ADESFA
- [ ] Validación de idempotencia cross-module (venta + AFIP + ANMAT)
- [ ] Flujo de remediación ANMAT completo

### 10. Tests de contrato entre submódulos
- [ ] `sgf-inventory` depende de `sgf-catalog` — integración correcta
- [ ] `sgf-pos` depende de `sgf-inventory` — reserva de stock al vender
- [ ] `sgf-integrations` depende de `sgf-pos` + `sgf-audit` — cross-cutting

## 🔧 Fixes de código pendientes

- [ ] **Actualizar imports en tests existentes** — reemplazar `com.sgf.modules.*` → nuevos packages
- [ ] **Verificar `SgfApplication` scan** — `scanBasePackages = "com.sgf"` cubre todos los submódulos
- [ ] **Verificar `@EntityScan` y `@EnableJpaRepositories`** — cubren los nuevos packages
- [ ] **Actualizar `settings.gradle` del root** — incluir `apps/api` como subproyecto si es necesario
- [ ] **Verificar docker-compose** — el volumen `certs` y paths siguen siendo válidos

## 🧪 Cobertura de tests deseada

| Módulo | Tests actuales | Tests necesarios | Cobertura objetivo |
|--------|---------------|-----------------|-------------------|
| sgf-core | 0 | 3-4 | 90%+ |
| sgf-sync | 0 | 6-7 | 85%+ |
| sgf-catalog | 0 | 3-4 | 85%+ |
| sgf-inventory | 0 | 4-5 | 85%+ |
| sgf-pos | 0 | 3-4 | 85%+ |
| sgf-audit | 0 | 2-3 | 80%+ |
| sgf-integrations | 3 (rotos) | 10-12 | 80%+ |
| sgf-app | 1 (roto) | 1 (e2e) | 70%+ |

**Total estimado:** ~35-40 tests nuevos por crear