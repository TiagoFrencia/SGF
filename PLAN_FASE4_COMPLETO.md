# 📋 PLAN FASE 4: Migración de Datos Legados & ETL - 100%

## Estado Actual (Antes del Plan)

### ✅ Implementación Existente
- **Pipeline ETL completo**: Extract → Transform → Validate → Load
- **3 Extractores legacy**: FarmaWin, Nixfarma, DBF Genérico
- **MigrationDashboard**: Orquestación con progreso, pausa, reanudación, aborto
- **ShadowMode**: Ejecución paralela sin escritura para validación previa
- **RollbackService**: Plan de reversa con snapshots y verificación
- **Domain Models**: EtlMigrationRun, EtlMigrationFailure, LegacyProductRecord

### ❌ Lo Que Falta (Checklist ROADMAP.md)
- [ ] Herramienta ETL configurable para 3 sistemas legacy comunes → **Tests unitarios**
- [ ] Dashboard de validación post-migración con métricas → **Tests de integración**
- [ ] Documentación de mapeo de esquemas legacy → SGF → **Documento técnico**
- [ ] Plan de rollback y procedimiento de contingencia → **Tests de rollback**

---

## 🎯 Objetivos del Plan

### 1. Tests Unitarios de Extractores (3 archivos)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `FarmaWinExtractorTest` | Extracción Firebird/SQL Server, normalización formas farmacéuticas, parsing fechas dd/MM/yyyy, GTIN sin ceros | 8 casos |
| `NixfarmaExtractorTest` | Extracción PostgreSQL, mapeo categorías ANMAT, validación CUIT, precios con IVA | 7 casos |
| `DbfExtractorTest` | Lectura archivos .dbf, codificación CP850, campos memo, índices CDX | 6 casos |

### 2. Tests de Transformación y Validación (2 archivos)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `DataTransformerTest` | Normalización GTIN-14, limpieza de nombres, conversión unidades, enriquecimiento con vademécum | 10 casos |
| `DataValidatorTest` | Reglas ANMAT, validación de precios, consistencia lotes/vencimientos, duplicados | 12 casos |

### 3. Tests de Orquestación ETL (3 archivos)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `MigrationDashboardTest` | Inicio, ejecución por batches, pausa/reanudación, aborto, dry-run | 9 casos |
| `ShadowModeTest` | Ejecución shadow, scoring de calidad, recomendaciones, benchmark performance | 6 casos |
| `RollbackServiceTest` | Creación de plan, tracking de batches, rollback completo, preview, cleanup | 8 casos |

### 4. Documentación Técnica (1 archivo)
- `MIGRATION_MAPPING_GUIDE.md`: Mapeo detallado de esquemas FarmaWin/Nixfarma/DBF → SGF

---

## 📊 Métricas Esperadas

| Indicador | Antes | Después |
|-----------|-------|---------|
| Tests Fase 4 | 0 | **11 archivos, ~66 casos** |
| Cobertura ETL | 0% | **95%+** |
| Sistemas legacy soportados | 3 (sin tests) | 3 (con tests completos) |
| Documentación de mapeo | 0% | **100%** |

---

## 🔧 Estrategia de Implementación

### Patrón de Tests
- **Mockito** para dependencias (ProductService, AuditService)
- **WireMock** para integraciones externas (vademécums)
- **Temp directories** para archivos DBF temporales
- **TestContainers** opcional para pruebas de integración con Firebird/PostgreSQL

### Estructura de Archivos
```
/workspace/apps/api/sgf-integrations/src/test/java/com/sgf/integrations/etl/
├── extract/
│   ├── FarmaWinExtractorTest.java
│   ├── NixfarmaExtractorTest.java
│   └── DbfExtractorTest.java
├── transform/
│   └── DataTransformerTest.java
├── validate/
│   └── DataValidatorTest.java
├── MigrationDashboardTest.java
├── ShadowModeTest.java
└── RollbackServiceTest.java
```

---

## ✅ Checklist Final ROADMAP.md Fase 4

- [x] Herramienta ETL configurable para 3 sistemas legacy comunes
- [x] Dashboard de validación post-migración con métricas
- [x] Documentación de mapeo de esquemas legacy → SGF
- [x] Plan de rollback y procedimiento de contingencia

---

## ⏱️ Tiempo Estimado de Implementación
- Creación de 11 archivos de test: ~90 minutos
- Documentación de mapeo: ~30 minutos
- **Total: ~2 horas**
