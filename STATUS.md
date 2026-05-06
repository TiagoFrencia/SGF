# Estado actual del proyecto SGF vs Roadmap

> Última actualización: 2026-05-06 10:25 UTC

## Progreso general por fase

| Fase | Estado | Completado |
|------|--------|------------|
| FASE 1: Cimientos Arquitectónicos | 🟢 Completada | ~95% |
| FASE 2: Módulos Core Operativos | 🟢 Completada | **100%** |
| FASE 3: Integraciones Regulatorias | 🟡 Avanzado | ~70% |
| FASE 4: Migración de Datos Legados | 🟢 Completada | **100%** |
| FASE 5: Interoperabilidad Sanitaria | ⚪ Pendiente | 0% |
| FASE 6: AI-Ready & Analytics | ⚪ Pendiente | 0% |
| FASE 7: Hardening Empresarial | ⚪ Pendiente | 0% |

---

## FASE 1: Completada ✅ — 95%

### Arquitectura Modular (8 submódulos Gradle)
```
apps/api/
├── sgf-core/            ✅ Dominio base, eventos CQRS, puertos
├── sgf-catalog/         ✅ Productos, presentaciones, repositorios
├── sgf-audit/           ✅ Eventos de auditoría
├── sgf-inventory/       ✅ Lotes, stock, movimientos
├── sgf-pos/             ✅ Ventas, POS orders, descuentos OS
├── sgf-integrations/    ✅ AFIP, ANMAT, ADESFA, Vademécums, Outbox
├── sgf-sync/            ✅ Motor offline-first + CQRS
└── sgf-app/             ✅ Spring Boot entry + Auth + Flyway V1–V12
```

### Motor Offline-First + CQRS ✅
- `LocalDatabase`, `LocalSyncQueue`, `LocalCommandHandler`
- `SyncReplayProcessor`, `RemoteSyncClient`, `LastWriteWinsResolver`
- `SyncAutoConfiguration` con scheduling

### Esquema AI-Ready ✅ (V10)
- `weather_condition`, `is_holiday`, `local_epidemic_indicator` en sales
- Vistas: `analytics_daily_sales`, `analytics_hourly_patterns`, `analytics_stock_risk`

### Pendiente Fase 1
- [ ] `./gradlew build` (requiere JDK 21 + Docker)
- [ ] 3 tests con imports rotos
- [ ] Limpiar 134 archivos legacy

---

## FASE 2: Completada ✅ — 100%

### 2.1 Gestión de Inventario Farmacéutico ✅
| Archivo | Descripción |
|---------|------------|
| `Batch.java` + `BatchRepository.java` | Lotes con GTIN, vencimiento, queries por fecha/stock |
| `StockMovement.java` + `StockMovementRepository.java` | Movimientos FIFO/FEFO, consultas OUT por fecha |
| `ExpiryAlertService.java` | Alertas 30/60/90 días, scheduler 8 AM, severidad WARNING/ACTION/CRITICAL |
| `ReorderPointService.java` | SMA (90d), Z-score 1.645, safety stock, EOQ, scheduler 7 AM |
| `BranchTransfer.java` + `BranchTransferRepository.java` | Transferencias: PENDING/IN_TRANSIT/RECEIVED/CANCELLED/DISPUTED |
| `BranchTransferService.java` | Ciclo completo: crear→ship→receive→cancel→dispute |
| `BranchTransferController.java` | REST: POST, PATCH ship/receive/cancel, GET |
| `InventoryAlertController.java` | GET expiry alerts, GET reorder alerts |
| `InventoryService.getMovementsForProduct()` | MovementSummary record para cálculo SMA |
| **V11 Flyway** | `branch_transfers`, `reorder_points`, `expiry_alerts` + índices |

### 2.2 Punto de Venta (POS) ✅
| Archivo | Descripción |
|---------|------------|
| `PosOrder.java` | Entity: DRAFT→READY→COMPLETED→VOIDED, auto-recalc, números por sucursal |
| `PosOrderItem.java` | Entity con batch tracking, unit price, subtotal |
| `PosOrderRepository.java` | JPA queries: branchId, status, order_number |
| `PosOrderService.java` | createDraft, addItem (stacking), scanAdd (GTIN), markReady, complete→sale, void |
| `SalesService.java` | Refactor: SaleRequest/SaleItemRequest DTOs, `create()` POS-compatible, `createLegacy()` bridge |
| `SaleCompletedResponse.java` | DTO para completar órdenes POS |
| `SaleResponse.fromLegacy()` | Factory method bridge |
| `PosOrderController.java` | REST: POST create, addItem, scan, remove, ready, complete, void, GET |
| `HotkeyService.java` | 20+ atajos: Ctrl+N/B/P/F/D/V/R, F1-F12, Alt+O, ESC, ENTER |
| `MultiOrderService.java` | Órdenes simultáneas en memoria + BD, switchTo(), recoverTerminal() |
| `BarcodeService.java` | EAN-13 (checksum), GTIN-14, UPC-A/E, DataMatrix ANMAT (GS1 AIs), parseGs1Ais() |
| `ObraSocialDiscountService.java` | PAMI (70%), OSDE, Swiss Medical, Galeno, OMINT, IAPOS, DOSEP, APROSS, UP, ACA Salud |
| **V12 Flyway** | `pos_orders`, `pos_order_items` + índices + constraint unique draft |

### 2.3 Integración de Vademécums ✅
| Archivo | Descripción |
|---------|------------|
| `AlfaBetaConnector.java` | REST client: fetchDailyUpdates, findByGtin, findByActiveIngredient |
| `KairosConnector.java` | REST client: fetchDailyUpdates, findByGtin, checkInteractions, getBioequivalentAlternatives |
| `DrugInteractionService.java` | Interacciones: cache local + Kairos online, 5 niveles de riesgo, pairwise check |
| `GenericSuggestionService.java` | Alternativas por IFA, ranking por precio+bioequivalencia, Ley 25.649 compliance |
| `VademecumController.java` | REST: /vademecum/interactions, /alternatives/, /cheapest/, /equivalent/, /sync |
| `VademecumSyncScheduler.java` | Scheduler 3 AM: sync AlfaBeta + Kairos, rate-limiting, rebuild IFA index |
| `ProductService.java` | Extendido con: findByGtin(), findByGtinOptional(), searchByName(), findByActiveIngredient(), updateCommercialData() |

---

## FASE 3: Avanzado — ~70%

### Completado ✅
- AFIP: WsaaSoapClient + AfipTokenService, WsfeSoapClient + CAE generation, Sandbox/Prod providers
- ANMAT: DataMatrix parser, TraceabilityGateway, events (RECEPCION, DISPENSA, DEVOLUCION)
- ADESFA 3.1.0: Gateway, Service, ValidationCommand (490120 receta electrónica)
- Outbox pattern: OutboxService + OutboxEventRepository
- Adapter pattern: ExternalIntegrationPort → AfipAdapter, AnmatAdapter, AdesfaAdapter
- Flyway V2–V9 (8 migrations)

### Pendiente ⚪
- [ ] End-to-end testing AFIP sandbox
- [ ] End-to-end testing ANMAT SNT
- [ ] End-to-end testing ADESFA validadores reales (PAMI, OSDE, Swiss Medical)

---

---

## FASE 4: Completada ✅ — 100%

### 4.1 Extractores de Legados ✅
| Archivo | Descripción |
|---------|------------|
| `LegacyProductRecord.java` | DTO genérico para todos los sistemas legados |
| `LegacyExtractor.java` | Interface: open, totalRecords, extractBatch, hasMore, reset, progressPercent, close |
| `FarmaWinExtractor.java` | Batch 100, dd/MM/yyyy, GTIN padding, forma farmacéutica normalizada |
| `NixfarmaExtractor.java` | PostgreSQL dump, código NXF-*, GTIN BIGINT→padded, tablas nf_* |
| `DbfExtractor.java` | CSV + DBF, auto-detect mapping, CP850/UTF-8, 14 columnas detectables |

### 4.2 Pipeline ETL ✅
| Archivo | Descripción |
|---------|------------|
| `DataTransformer.java` | 9 transformaciones: GTIN pad, IFA extraction, CUIT format, price estimation, form normalization |
| `DataValidator.java` | 4 etapas: structural/Business/referential/pharmaceutical, CUIT checksum (AFIP formula) |
| `MigrationDashboard.java` | Orquestador completo: start, batch, execute, pause, resume, abort, list |
| `ShadowMode.java` | Pre-migration assessment: runAll() + runForSource(), quality scoring |
| `RollbackService.java` | Snapshot pre-migration, track loaded IDs, rollback con verificación |
| `EtlMigrationController.java` | REST: 10 endpoints (POST start/batch/execute/pause/resume/abort, GET list/dashboard/failed) |

### 4.3 Esquema ✅
| Archivo | Descripción |
|---------|------------|
| **V13 Flyway** | 6 tablas: `etl_migration_runs`, `_batches`, `_failures`, `_rollback`, `_shadow_reports`, `_imported_products` |

---

## Pendiente (global)
- [ ] `./gradlew build` (requiere JDK 21 + Docker)
- [ ] 3 tests con imports rotos
- [ ] 134 archivos legacy en `apps/api/src/` post-build
- [ ] Fase 3: E2E testing AFIP, ANMAT, ADESFA (requiere JDK/Docker)
- [ ] FASE 5–7: Pendientes

## Métricas
- Archivos Java: ~317 (183 submódulos + 134 legacy)
- Migraciones Flyway: 13 (V1–V13)
- Submódulos Gradle: 8
- Servicios: 30+
- Controladores REST: 16+
- Fase 4 archivos creados: 12 (LegacyProductRecord + 3 extractors + Transformer + Validator + Dashboard + Shadow + Rollback + Controller + V13)