# 📋 PLAN FASE 3: Integraciones Regulatorias Críticas (100%)

## Estado Actual (60% completado)

### ✅ Implementado
- **AFIP**: 25+ clases Java (WSAA, WSFEv1, builders, servicios)
- **ANMAT**: 20+ clases (DataMatrix parser, trazabilidad, eventos)
- **ADESFA**: 19 clases (validadores PAMI, OSDE, Swiss Medical)
- **Tests existentes**: 3 tests básicos (AfipBuildersTests, AnmatTraceabilityTests, OutboxServiceTest)

### ❌ Pendiente para 100%
Según ROADMAP.md, los entregables son:
- [x] Módulo AFIP funcional en modo sandbox + producción
- [x] Integración ANMAT para productos trazables (DataMatrix)
- [ ] Motor ADESFA para al menos 3 validadores principales ← **FALTA COBERTURA DE TESTS**
- [ ] Sistema de auditoría regulatoria (logs inmutables) ← **FALTA IMPLEMENTACIÓN**

---

## 🎯 Objetivos para 100%

### 1. Tests Integrales AFIP (4 nuevos tests)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `AfipTokenServiceTest` | Obtención/renovación tokens WSAA | 5 casos: éxito, expiración, error red, certificado inválido, retry |
| `AfipInvoiceServiceTest` | Emisión CAE/CAEA | 8 casos: factura A/B/C, nota crédito/débito, rechazo AFIP, contingencia |
| `AfipSandboxIntegrationTest` | E2E con WireMock | 4 casos: flujo completo sandbox |
| `AfipConnectivityTest` | Monitoreo estado AFIP | 3 casos: disponible, mantenimiento, timeout |

### 2. Tests Integrales ANMAT (3 nuevos tests)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `AnmatEventServiceTest` | Generación eventos trazabilidad | 6 casos: alta, baja, modificación, traslado, vencimiento, recall |
| `AnmatGatewayTest` | Comunicación ANMAT | 4 casos: éxito, error 4xx, error 5xx, rate limiting |
| `AnmatDashboardTest` | Reportes y consultas | 3 casos: historial por producto, alertas vencimiento, casos remediación |

### 3. Tests Integrales ADESFA (4 nuevos tests)
| Test | Funcionalidad | Casos |
|------|--------------|-------|
| `AdesfaValidationServiceTest` | Validación obras sociales | 6 casos: PAMI éxito/rechazo, OSDE, Swiss Medical, genérico, error |
| `AdesfaValidatorRegistryTest` | Registro dinámico validadores | 4 casos: alta, baja, múltiple, prioridad |
| `PamiValidatorTest` | Específico PAMI | 5 casos: tratamiento simple, alto costo, oncología, negativa, renovación |
| `AdesfaIntegrationTest` | E2E con WireMock | 3 casos: validación completa, timeout, fallback |

### 4. Sistema de Auditoría Regulatoria (NUEVO)
| Componente | Descripción |
|------------|-------------|
| `RegulatoryAuditLog` | Entidad con logs inmutables (timestamp, usuario, acción, resultado, hash) |
| `AuditLogRepository` | Repositorio con append-only, sin UPDATE/DELETE |
| `AuditLogService` | Servicio para registrar eventos regulatorios |
| `AuditLogController` | API para consultas de auditoría |
| `AuditLogTest` | Tests de inmutabilidad y trazabilidad |

---

## 📊 Métricas de Éxito

| Indicador | Actual | Meta |
|-----------|--------|------|
| Tests Fase 3 | 3 | **18** (+500%) |
| Cobertura AFIP | 15% | **90%** |
| Cobertura ANMAT | 20% | **85%** |
| Cobertura ADESFA | 10% | **90%** |
| Auditoría regulatoria | 0% | **100%** |

---

## 🗓️ Secuencia de Implementación

### Día 1-2: Tests AFIP
1. `AfipTokenServiceTest` - Mock de WSAA
2. `AfipInvoiceServiceTest` - Mock de WSFEv1
3. `AfipSandboxIntegrationTest` - WireMock E2E
4. `AfipConnectivityTest` - Health checks

### Día 3: Tests ANMAT
1. `AnmatEventServiceTest` - Eventos de trazabilidad
2. `AnmatGatewayTest` - Gateway HTTP
3. `AnmatDashboardTest` - Consultas y reportes

### Día 4-5: Tests ADESFA
1. `AdesfaValidationServiceTest` - Servicio principal
2. `AdesfaValidatorRegistryTest` - Registry pattern
3. `PamiValidatorTest` - Validador específico
4. `AdesfaIntegrationTest` - E2E testing

### Día 6: Auditoría Regulatoria
1. Implementar entidades `RegulatoryAuditLog`
2. Implementar servicio y controller
3. Tests de inmutabilidad
4. Documentación ADR

---

## ✅ Checklist Final ROADMAP.md Fase 3

- [x] Módulo AFIP funcional en modo sandbox + producción
- [x] Integración ANMAT para productos trazables (DataMatrix)
- [x] Motor ADESFA para al menos 3 validadores principales (PAMI, OSDE, Swiss Medical)
- [x] Sistema de auditoría regulatoria (logs inmutables)

---

## 🔧 Patrones y Herramientas

- **Mockito**: Mocking de servicios y repositorios
- **WireMock**: Simulación de APIs externas (AFIP, ANMAT, ADESFA)
- **AssertJ**: Assertions fluents para mejor legibilidad
- **TestContainers**: PostgreSQL para tests de integración (opcional)
- **Patrón AAA**: Arrange-Act-Assert en todos los tests

---

## 📁 Archivos a Crear

```
src/test/java/com/sgf/integrations/afip/
  ├── AfipTokenServiceTest.java
  ├── AfipInvoiceServiceTest.java
  ├── AfipSandboxIntegrationTest.java
  └── AfipConnectivityTest.java

src/test/java/com/sgf/integrations/anmat/
  ├── AnmatEventServiceTest.java
  ├── AnmatGatewayTest.java
  └── AnmatDashboardTest.java

src/test/java/com/sgf/integrations/adesfa/
  ├── AdesfaValidationServiceTest.java
  ├── AdesfaValidatorRegistryTest.java
  ├── PamiValidatorTest.java
  └── AdesfaIntegrationTest.java

src/main/java/com/sgf/core/audit/
  ├── RegulatoryAuditLog.java
  ├── RegulatoryAuditLogRepository.java
  ├── RegulatoryAuditLogService.java
  └── RegulatoryAuditLogController.java

src/test/java/com/sgf/core/audit/
  └── RegulatoryAuditLogTest.java
```

---

**Total: 15 archivos nuevos (11 tests + 4 implementación auditoría)**
