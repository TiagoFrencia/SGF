# 🚀 PLAN DE EJECUCIÓN: SGF al 100% - ACTUALIZADO

**Fecha de Inicio**: 2025-01-09
**Equipo**: Backend Sr, Frontend Sr, QA Automation Sr, DevOps Sr
**Duración Estimada**: 10-12 semanas
**Progreso Actual**: 60-65%

---

## 📊 ESTADO ACTUAL VERIFICADO DEL PROYECTO

### ✅ Completado por Fase
- **FASE 1**: 100% - Cimientos arquitectónicos ✅
- **FASE 2**: 95% - Módulos core operativos ✅
- **FASE 3**: 90% - Integraciones regulatorias (AFIP, ANMAT, ADESFA) ✅
- **FASE 4**: 75% - ETL de migración (implementado, faltan tests) ⚠️
- **FASE 5**: 50% - Interoperabilidad sanitaria (HL7 FHIR base, falta PAMI/REFEPS) ⚠️
- **FASE 6**: 40% - IA (servicios implementados, falta entrenamiento/datos) ⚠️
- **FASE 7**: 15% - Hardening (infraestructura base lista, falta seguridad/performance) 🔴

### 📈 Métricas Actuales REALES
- **Archivos Java**: 213 (main) + 14 (test) = 227 total
- **Servicios/Repositorios**: 60 clases @Service/@Repository/@Component
- **Controladores REST**: 15 @RestController
- **Cobertura de Tests**: ~6% (14 tests para 213 clases) 🔴 CRÍTICO
- **Módulos Frontend**: 27 archivos TypeScript
- **Módulos Backend**: 9 módulos Spring Boot

### 📁 Distribución de Código
| Módulo | Archivos Main | Archivos Test | Cobertura |
|--------|--------------|---------------|-----------|
| sgf-core | 16 | 2 | 12.5% |
| sgf-sync | 9 | 2 | 22% |
| sgf-inventory | ~20 | 1 | 5% |
| sgf-audit | ~8 | 1 | 12.5% |
| sgf-catalog | ~15 | 1 | 6.7% |
| sgf-integrations | 119 | 4 | 3.4% |
| sgf-pos | ~15 | 1 | 6.7% |
| sgf-ai | 6 | 0 | 0% 🔴 |
| sgf-app | ~11 | 1 | 9% |

---

## 🎯 OBJETIVOS PRIORIZADOS

### **PRIORIDAD CRÍTICA - SEMANA 1-2: Tests y CI/CD**

#### 🔧 Backend Sr - Tests Masivos
1. **Reparar tests existentes** (12 archivos)
   - Verificar imports y dependencias
   - Actualizar mocks desactualizados
   - Validar configuración de test

2. **Tests AFIP** (5 nuevos tests)
   - `WsaaSoapClientTest.java` - Autenticación WSAA
   - `WsfeSoapClientTest.java` - Facturación WSFEv1
   - `AfipServiceIntegrationTest.java` - Flujo completo CAE
   - `AfipInvoiceTypeTest.java` - Tipos de factura
   - `AfipSandboxTest.java` - Modo sandbox

3. **Tests ANMAT** (3 nuevos tests)
   - `AnmatDataMatrixParserTest.java` - Parsing GS1 DataMatrix
   - `AnmatTraceabilityServiceTest.java` - Eventos de trazabilidad
   - `AnmatEventSerializationTest.java` - Serialización XML/JSON

4. **Tests ADESFA** (3 nuevos tests)
   - `AdesfaValidatorRegistryTest.java` - Registro validadores
   - `PamiValidatorTest.java` - Validador PAMI
   - `OsdeSwissMedicalValidatorTest.java` - OSDE y Swiss Medical

5. **Tests ETL** (4 nuevos tests)
   - `FarmaWinExtractorTest.java` - Extractor FarmaWin
   - `NixfarmaExtractorTest.java` - Extractor Nixfarma
   - `DataTransformerTest.java` - Transformación datos
   - `RollbackServiceTest.java` - Rollback migrations

6. **Tests por Módulo** (15+ tests)
   - sgf-core: EventBus, TenantContext, Domain Events
   - sgf-inventory: StockMovement, Batch, InventoryService
   - sgf-catalog: Product, Pricing, ProductService
   - sgf-pos: SalesService, Cart, Checkout
   - sgf-sync: SyncQueue, ConflictResolver, LocalDatabase
   - sgf-audit: AuditService, AuditEventListener
   - sgf-ai: ForecastingService, FraudDetectionService, AnomalyDetector

#### 🧪 QA Automation Sr - CI/CD Pipeline
1. **Mejorar GitHub Actions**
   - Agregar reporte JaCoCo obligatorio
   - Configurar SonarQube con quality gate
   - Tests paralelos por módulo para velocidad

2. **Tests E2E con Testcontainers**
   - `SalesToAfipE2ETest.java` - Venta → Factura AFIP
   - `AnmatTraceabilityE2ETest.java` - Trazabilidad completa
   - `OfflineSyncE2ETest.java` - Sincronización offline
   - `MigrationE2ETest.java` - Migración ETL completa

3. **Pruebas de Carga**
   - Configurar k6 en CI
   - 50+ usuarios concurrentes en POS
   - Validar latencia <200ms p95

#### 📊 Entregables Semana 2
- [ ] 0 tests rotos
- [ ] 40+ tests nuevos implementados
- [ ] Cobertura backend >60%
- [ ] Pipeline CI/CD con SonarQube funcional
- [ ] 5+ tests E2E implementados
- [ ] Pruebas de carga configuradas

---

### **PRIORIDAD ALTA - SEMANA 3-5: Interoperabilidad Sanitaria**

#### 🔧 Backend Sr - FASE 5 Completa
1. **Conector PAMI** (Nuevo módulo)
   - `PamiSiafarClient.java` - Conexión SIAFAR
   - `PamiValidaCofaService.java` - Validación COFA
   - `PamiResponseParser.java` - Parsing respuestas
   - Tests de integración con sandbox PAMI

2. **Integración REFEPS** (Nuevo módulo)
   - `RefepsMatriculaClient.java` - Validación matrículas
   - `RefepsProfessionalVerifier.java` - Verificación profesionales
   - Tests de validación de matrículas

3. **Módulo ReNaPDiS** (Ya existe cliente, completar)
   - `RenapdisPsychotropicService.java` - Psicofármacos
   - `RenapdisReportGenerator.java` - Reportes regulatorios
   - Tests de presentación de psicofármacos

4. **Recetas Digitales HL7 FHIR** (Completar)
   - `DigitalSignatureService.java` - Firma digital recetas
   - `FhirRecipeRepository.java` - Almacenamiento seguro
   - `FhirAntecedentsApi.java` - API antecedentes médicos
   - Auditoría de accesos a recetas

5. **Consentimiento Electrónico** (Nuevo)
   - `ConsentimientoDomain.java` - Modelo de dominio
   - `ConsentimientoRepository.java` - Repositorio
   - `ConsentimientoController.java` - Endpoints REST
   - `ConsentimientoAuditService.java` - Auditoría accesos

#### 🎨 Frontend Sr - Componentes Angular
1. **Dashboard Reportes Avanzados**
   - Gráficos de ventas por período
   - Reportes de stock y vencimientos
   - Exportación PDF/Excel

2. **Configuración Multi-Sucursal**
   - CRUD de sucursales
   - Configuración de tenant
   - Permisos por sucursal

3. **Módulo Interoperabilidad**
   - Panel de conexiones PAMI/REFEPS/ReNaPDiS
   - Estado de sincronización
   - Logs de transacciones

4. **POS Mobile-Responsive**
   - Diseño responsive para tablets/móviles
   - Offline-first robusto
   - Atajos de teclado personalizables

5. **Integración APIs IA**
   - Dashboard forecasting de stock
   - Alertas de fraude en tiempo real
   - Sugerencias de sustitución visual

#### 📊 Entregables Semana 5
- [ ] Conectores PAMI, REFEPS, ReNaPDiS implementados y testeado
- [ ] Módulo recetas digitales funcional con firma
- [ ] Consentimiento electrónico operacional
- [ ] Frontend reportes y configuración completo
- [ ] POS mobile-responsive probado en dispositivos
- [ ] Cobertura tests >75%

---

### **PRIORIDAD MEDIA - SEMANA 6-7: IA Operativa**

#### 🔧 Backend Sr - IA Producción
1. **Pipeline Forecasting**
   - Generar dataset histórico simulado (1000+ productos)
   - Entrenar modelo Prophet/ARIMA
   - Exportar a formato ONNX
   - `ForecastingTrainingService.java` - Entrenamiento
   - `OnnxInferenceService.java` - Inferencia en producción

2. **Detección de Fraude**
   - `FraudRuleEngine.java` - Reglas de negocio
   - `AnomalyDetectionService.java` - Modelo anomalías
   - `ShapExplainerService.java` - Explicabilidad SHAP
   - `FraudAlertNotificationService.java` - Notificaciones

3. **OCR Recetas MVP**
   - Integrar Tesseract4J
   - `RecipeOcrService.java` - OCR de recetas
   - `OcrConfidenceValidator.java` - Validación confianza
   - Flujo confirmación humana baja confianza

#### 🧪 QA Automation Sr - Validación IA
1. **Tests Modelos IA**
   - Precisión forecasting >85%
   - Detección anomalías: recall >90%
   - Tests estrés inferencia ONNX

2. **Performance IA**
   - Latencia inferencia <200ms
   - Throughput 100+ solicitudes simultáneas
   - Uso memoria/CPU bajo carga

#### 📊 Entregables Semana 7
- [ ] Modelo forecasting activo con datos simulados
- [ ] Sistema detección fraude operacional
- [ ] OCR recetas MVP funcional
- [ ] Tests rendimiento IA aprobados
- [ ] Cobertura tests IA >80%

---

### **PRIORIDAD BAJA - SEMANA 8-12: Hardening Empresarial**

#### ⚙️ DevOps Sr - Seguridad y Performance
1. **Seguridad OWASP**
   - Ejecutar OWASP ZAP contra staging
   - Corregir vulnerabilidades críticas/altas
   - Verificar encriptación TLS 1.3
   - Auditoría logs inmutables

2. **Performance Kubernetes**
   - HPA configurado y probado (ya existe)
   - Redis caché para consultas frecuentes
   - Connection pooling optimizado
   - Alertas Prometheus: latencia >200ms

3. **CI/CD Avanzado**
   - Deploy automatizado staging/producción
   - Blue-green deployments
   - Rollback automático ante fallos
   - Canary releases

#### 🔧 Backend Sr - Optimización
1. **Profiling y Optimización**
   - Identificar queries lentas (>100ms)
   - Indexación estratégica DB
   - Limpieza código muerto
   - Optimizar serialización JSON

2. **Documentación Final**
   - OpenAPI/Swagger actualizado
   - Guías despliegue paso a paso
   - Manual técnico arquitectura
   - Runbooks operación

#### 🎨 Frontend Sr - Pulido UI
1. **Testing Cross-Browser**
   - Chrome, Firefox, Safari, Edge
   - Testing en móviles reales

2. **Accesibilidad WCAG 2.1 AA**
   - Contraste de colores
   - Navegación teclado
   - Screen readers

3. **Optimización Bundle**
   - Lazy loading módulos
   - Tree shaking
   - Compresión assets

#### 🧪 QA Automation Sr - Batería Final
1. **Cobertura Total**
   - Cobertura >85% backend
   - Cobertura >70% frontend

2. **Tests Regresión**
   - Suite regresión automatizada
   - 0 bugs críticos conocidos

3. **Pruebas Finales**
   - Load testing 100 transacciones/segundo
   - Soak testing 24 horas
   - Chaos engineering básico

#### 📊 Entregables Semana 12
- [ ] Seguridad validada OWASP (0 críticas, 0 altas)
- [ ] Performance: 100 transacciones/segundo estables
- [ ] CI/CD fully automated con blue-green
- [ ] Documentación 100% completa
- [ ] Cobertura tests >85%
- [ ] **PROYECTO 100% COMPLETADO** ✅

---

## 🛠️ HERRAMIENTAS CONFIGURADAS

### Desarrollo
- ✅ **IDE**: IntelliJ IDEA / VS Code
- ✅ **Control Versiones**: Git + GitHub
- ✅ **Build**: Gradle 8.5 + JDK 21
- ⏳ **Gestión Tareas**: GitHub Projects (por configurar)

### Calidad
- ✅ **Tests Unitarios**: JUnit 5 + Mockito
- ✅ **Tests E2E**: Testcontainers + Spring Boot Test
- ⏳ **Cobertura**: JaCoCo (configurar en CI)
- ⏳ **Calidad Código**: SonarQube (requiere secrets)

### Seguridad
- ⏳ **SAST**: SonarQube Security
- ⏳ **DAST**: OWASP ZAP
- ✅ **Dependencias**: Gradle dependency verification

### Infraestructura
- ✅ **Contenedores**: Docker + Docker Compose
- ✅ **Orquestación**: Kubernetes (HPA configurado)
- ✅ **Monitoreo**: Prometheus + Grafana (dashboards listos)
- ⏳ **Logs**: ELK Stack (pendiente)
- ✅ **Caché**: Redis deployment configurado

### Frontend
- ✅ **Framework**: Angular 17+
- ✅ **UI Library**: Angular Material + PrimeNG
- ⏳ **Testing**: Jasmine + Karma (verificar configuración)
- ⏳ **E2E**: Cypress (pendiente)

---

## ⚠️ RIESGOS Y MITIGACIÓN ACTUALIZADA

| Riesgo | Impacto | Probabilidad | Mitigación |
|--------|---------|--------------|------------|
| Cobertura tests insuficiente (6%) | Crítico | Alta | 40+ tests semana 1-2, CI obligatorio |
| Complejidad integraciones SOAP (PAMI) | Alto | Media | Mocks desarrollo, sandbox testing |
| Datos insuficientes para IA | Alto | Media | Generar dataset simulado 1000+ productos |
| Burnout equipo | Crítico | Media | Sprints 2 semanas, priorización clara |
| Deuda técnica acumulada | Medio | Alta | 20% tiempo refactorización |
| Cambios regulatorios | Alto | Media | Arquitectura adapters, monitoreo RSS |
| Secrets CI/CD faltantes | Alto | Alta | Documentar secrets necesarios |

---

## 📈 MÉTRICAS DE ÉXITO ACTUALIZADAS

### Línea Base Actual
| Área | Actual | Meta Semana 2 | Meta Semana 5 | Meta Semana 7 | Meta Final |
|------|--------|---------------|---------------|---------------|------------|
| **Cobertura Tests** | 6% | 60% | 75% | 80% | >85% |
| **Tests Totales** | 14 | 55+ | 70+ | 85+ | 100+ |
| **Bugs Críticos** | Desconocido | 0 | 0 | 0 | 0 |
| **Latencia API** | No medida | <300ms | <250ms | <200ms | <200ms p95 |
| **Transacciones/seg** | No medido | 50+ | 75+ | 100+ | 100+ |
| **FASE 5 Interop** | 50% | 75% | 100% | 100% | 100% |
| **FASE 6 IA** | 40% | 40% | 60% | 100% | 100% |
| **FASE 7 Hardening** | 15% | 25% | 40% | 60% | 100% |

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS - DÍA 1

### ✅ Completar Hoy
1. **[Backend] Listar todos los servicios sin tests** → EN PROGRESO
2. **[QA] Verificar CI pipeline actual** → COMPLETADO (ci.yml existe)
3. **[DevOps] Auditar configuración k8s** → COMPLETADO (HPA ya configurado)
4. **[Frontend] Listar componentes pendientes** → EN PROGRESO

### 📅 Día 2-3
1. **[Backend] Crear 10 tests base por módulo**
2. **[QA] Agregar JaCoCo al pipeline CI**
3. **[Backend] Tests AFIP: WSAA y WSFEv1**
4. **[Frontend] Iniciar dashboard reportes**

### 📅 Día 4-5
1. **[Backend] Tests ANMAT y ADESFA**
2. **[QA] Primer test E2E: Venta→AFIP→ANMAT**
3. **[DevOps] Configurar SonarQube (si hay secrets)**
4. **[Frontend] Módulo configuración sucursales**

---

## 📋 CHECKLIST DETALLADO DE TESTS POR IMPLEMENTAR

### Módulo sgf-core (2 tests existentes → 8 tests)
- [x] BaseEntityTest.java
- [x] EventBusTest.java
- [ ] TenantContextTest.java
- [ ] TenantFilterTest.java
- [ ] CorrelationIdFilterTest.java
- [ ] DomainEventSerializationTest.java
- [ ] ConflictExceptionTest.java
- [ ] NotFoundExceptionTest.java
- [ ] BadRequestExceptionTest.java

### Módulo sgf-integrations (4 tests existentes → 20 tests)

#### AFIP (1 test existente → 6 tests)
- [x] AfipBuildersTests.java
- [ ] WsaaSoapClientTest.java
- [ ] WsfeSoapClientTest.java
- [ ] AfipServiceIntegrationTest.java
- [ ] AfipInvoiceTypeTest.java
- [ ] AfipSandboxTest.java

#### ANMAT (1 test existente → 4 tests)
- [x] AnmatTraceabilityTests.java
- [ ] AnmatDataMatrixParserTest.java
- [ ] AnmatTraceabilityServiceTest.java
- [ ] AnmatEventSerializationTest.java

#### ADESFA (0 tests → 4 tests)
- [ ] AdesfaValidatorRegistryTest.java
- [ ] PamiValidatorTest.java
- [ ] OsdeValidatorTest.java
- [ ] SwissMedicalValidatorTest.java

#### ETL (0 tests → 5 tests)
- [ ] FarmaWinExtractorTest.java
- [ ] NixfarmaExtractorTest.java
- [ ] DataTransformerTest.java
- [ ] RollbackServiceTest.java
- [ ] ShadowModeTest.java

#### Vademécum (0 tests → 2 tests)
- [ ] DrugInteractionServiceTest.java
- [ ] VademecumSyncSchedulerTest.java

### Módulo sgf-inventory (1 test existente → 6 tests)
- [x] InventoryServiceTest.java
- [ ] StockMovementTest.java
- [ ] BatchTest.java
- [ ] BranchTransferServiceTest.java
- [ ] ExpiryAlertServiceTest.java
- [ ] ReorderPointServiceTest.java

### Módulo sgf-catalog (1 test existente → 5 tests)
- [x] ProductServiceTest.java
- [ ] ProductTest.java
- [ ] PricingServiceTest.java
- [ ] ProductPresentationTest.java
- [ ] CategoryServiceTest.java

### Módulo sgf-pos (1 test existente → 6 tests)
- [x] SalesServiceTest.java
- [ ] CartTest.java
- [ ] CheckoutServiceTest.java
- [ ] PaymentServiceTest.java
- [ ] ReceiptGenerationTest.java
- [ ] PosOfflineModeTest.java

### Módulo sgf-sync (2 tests existentes → 5 tests)
- [x] LastWriteWinsResolverTest.java
- [x] LocalCommandHandlerTest.java
- [ ] LocalSyncQueueTest.java
- [ ] RemoteSyncClientTest.java
- [ ] SyncReplayProcessorTest.java

### Módulo sgf-audit (1 test existente → 4 tests)
- [x] AuditServiceTest.java
- [ ] AuditEventTest.java
- [ ] AuditEventListenerTest.java
- [ ] AuditSearchServiceTest.java

### Módulo sgf-ai (0 tests → 6 tests) 🔴 CRÍTICO
- [ ] ForecastingServiceTest.java
- [ ] FraudDetectionServiceTest.java
- [ ] AnomalyDetectorTest.java
- [ ] OnnxModelLoaderTest.java
- [ ] ShapExplainerTest.java
- [ ] AiControllerTest.java

### Módulo sgf-app (1 test existente → 3 tests)
- [x] SgfApiApplicationTests.java
- [ ] ApiHealthIntegrationTest.java
- [ ] ApiCorsConfigurationTest.java

### Tests E2E (0 → 8 tests) 🔴 CRÍTICO
- [ ] SalesToAfipE2ETest.java
- [ ] AnmatTraceabilityE2ETest.java
- [ ] AdesfaValidationE2ETest.java
- [ ] OfflineSyncE2ETest.java
- [ ] MigrationEtlE2ETest.java
- [ ] InventoryReceptionE2ETest.java
- [ ] UserAuthenticationE2ETest.java
- [ ] MultiTenantIsolationE2ETest.java

**Total Tests Planificados**: 100+ tests

---

## 🔐 SECRETS NECESARIOS PARA CI/CD

Los siguientes secrets deben configurarse en GitHub Repository Settings → Secrets:

```bash
# SonarQube
SONAR_TOKEN=<token_de_sonarqube_cloud_o_self_hosted>
SONAR_HOST_URL=https://sonarcloud.io  # o URL self-hosted

# Docker Registry (para producción)
DOCKER_USERNAME=<usuario_docker>
DOCKER_PASSWORD=<password_docker>

# Kubernetes (para deploy automático)
KUBE_CONFIG=<kubeconfig_base64>

# Variables de entorno para tests
AFIP_SANDBOX_CERT=<certificado_sandbox_afip>
AFIP_SANDBOX_KEY=<clave_sandbox_afip>
```

---

**Nota**: Este plan es dinámico y se ajustará según el progreso real. Revisiones diarias las primeras 2 semanas, luego semanales.

---

## 🎯 OBJETIVOS POR SEMANA

### **SEMANA 1-2: Consolidación del Core (QA + Backend)**

#### 🔧 Backend Sr
1. **Reparar tests rotos** (~12 archivos)
   - Actualizar imports obsoletos
   - Corregir mocks desactualizados
   - Validar integración con servicios reales

2. **Expandir cobertura de tests**
   - Agregar tests para servicios AFIP (WSAA, WSFEv1)
   - Tests de integración ANMAT (DataMatrix parsing)
   - Tests ADESFA (validadores PAMI, OSDE, Swiss Medical)

3. **Completar módulos ETL**
   - Tests de integración para extractores (FarmaWin, Nixfarma)
   - Validación de transformación de datos
   - Pruebas de rollback y shadow mode

#### 🧪 QA Automation Sr
1. **Configurar pipeline de CI/CD**
   - GitHub Actions para ejecutar tests en cada commit
   - Reportes de cobertura con JaCoCo
   - Validación de calidad con SonarQube

2. **Crear tests de integración E2E**
   - Flujo completo: Venta → Factura AFIP → Trazabilidad ANMAT
   - Pruebas de carga para POS (50+ usuarios concurrentes)
   - Validación de sincronización offline-first

3. **Automatizar pruebas regulatorias**
   - Sandbox AFIP: validar CAE, tipos de factura
   - ANMAT: verificar eventos de trazabilidad
   - ADESFA: testear validación de obras sociales

#### 📊 Entregables Semana 2
- [ ] 0 tests rotos
- [ ] Cobertura backend >70%
- [ ] Pipeline CI/CD funcional
- [ ] 10+ tests E2E implementados

---

### **SEMANA 3-5: Interoperabilidad Sanitaria (Backend + Frontend)**

#### 🔧 Backend Sr
1. **Completar FASE 5 - Interoperabilidad**
   - Implementar cliente PAMI (SIAFAR/ValidaCOFA)
   - Integración REFEPS para validación de matrículas
   - Módulo ReNaPDiS para psicofármacos

2. **Recetas Digitales HL7 FHIR**
   - Firma digital de recetas
   - Almacenamiento seguro con auditoría
   - API para lectura/escritura de antecedentes

3. **Consentimiento Electrónico**
   - Modelo de dominio para consentimientos
   - Endpoints REST para gestión
   - Auditoría de accesos

#### 🎨 Frontend Sr
1. **Completar componentes Angular**
   - Dashboard de reportes avanzados
   - Configuración de sucursales multi-tenant
   - Módulo de interoperabilidad sanitaria

2. **Mejorar UX del POS**
   - Interfaces responsive para móviles
   - Capacidades offline-first robustas
   - Atajos de teclado personalizables

3. **Integrar APIs de IA**
   - Dashboard de forecasting de stock
   - Alertas de detección de fraude
   - Visualización de sugerencias de sustitución

#### 📊 Entregables Semana 5
- [ ] Conectores PAMI, REFEPS, ReNaPDiS implementados
- [ ] Módulo de recetas digitales funcional
- [ ] Frontend de reportes y configuración completo
- [ ] POS mobile-responsive probado

---

### **SEMANA 6-7: Inteligencia Artificial Operativa (Backend + QA)**

#### 🔧 Backend Sr
1. **Pipeline de Forecasting**
   - Entrenar modelo con datos históricos simulados
   - Integrar modelo ONNX en servicio de alertas
   - Endpoint para predicciones de reposición

2. **Detección de Fraude**
   - Combinar reglas de negocio + modelo de anomalías
   - Dashboard de alertas con explicabilidad SHAP
   - Sistema de notificaciones para desvíos

3. **OCR de Recetas (MVP)**
   - Integrar Tesseract o similar
   - Flujo de confirmación humana para baja confianza
   - Almacenamiento de recetas digitalizadas

#### 🧪 QA Automation Sr
1. **Validar modelos de IA**
   - Tests de precisión para forecasting
   - Validación de detección de anomalías
   - Pruebas de estrés para inferencia ONNX

2. **Pruebas de rendimiento**
   - Latencia de inferencia <200ms
   - Throughput para 100+ solicitudes simultáneas
   - Uso de memoria y CPU bajo carga

#### 📊 Entregables Semana 7
- [ ] Modelo de forecasting activo en sandbox
- [ ] Sistema de detección de fraude operacional
- [ ] OCR de recetas funcional (MVP)
- [ ] Tests de rendimiento de IA aprobados

---

### **SEMANA 8-10: Hardening Empresarial (DevOps + Todos)**

#### ⚙️ DevOps Sr
1. **Seguridad y Cumplimiento**
   - Ejecutar OWASP Top 10 con ZAP
   - Verificar encriptación de datos sensibles
   - Auditoría de logs inmutables

2. **Performance y Escalabilidad**
   - Configurar HPA en Kubernetes
   - Implementar Redis para caché
   - Alertas Prometheus/Grafana para latencia >200ms

3. **CI/CD Avanzado**
   - Deploy automatizado a staging/producción
   - Blue-green deployments
   - Rollback automático ante fallos

#### 🔧 Backend Sr
1. **Optimización de código**
   - Profiling de consultas lentas
   - Indexación de base de datos
   - Limpieza de código muerto

2. **Documentación final**
   - OpenAPI/Swagger actualizado
   - Guías de despliegue
   - Manual técnico de arquitectura

#### 🎨 Frontend Sr
1. **Pulido final de UI**
   - Testing cross-browser
   - Accesibilidad (WCAG 2.1 AA)
   - Optimización de bundle size

#### 🧪 QA Automation Sr
1. **Batería final de tests**
   - Cobertura >85%
   - 0 bugs críticos conocidos
   - Pruebas de regresión automatizadas

#### 📊 Entregables Semana 10
- [ ] Seguridad validada (OWASP)
- [ ] Performance: 100 transacciones/segundo
- [ ] CI/CD fully automated
- [ ] Documentación completa
- [ ] **PROYECTO 100% COMPLETADO** ✅

---

## 🛠️ HERRAMIENTAS Y RECURSOS

### Desarrollo
- **IDE**: IntelliJ IDEA / VS Code
- **Control de Versiones**: Git + GitHub
- **Gestión de Tareas**: GitHub Projects

### Calidad
- **Tests Unitarios**: JUnit 5 + Mockito
- **Tests E2E**: Testcontainers + Spring Boot Test
- **Cobertura**: JaCoCo
- **Calidad de Código**: SonarQube

### Seguridad
- **SAST**: SonarQube Security
- **DAST**: OWASP ZAP
- **Dependencias**: Snyk / Dependabot

### Infraestructura
- **Contenedores**: Docker + Docker Compose
- **Orquestación**: Kubernetes (k8s/)
- **Monitoreo**: Prometheus + Grafana
- **Logs**: ELK Stack (pendiente)

### Frontend
- **Framework**: Angular 17+
- **UI Library**: Angular Material + PrimeNG
- **Testing**: Jasmine + Karma
- **E2E**: Cypress (pendiente)

---

## ⚠️ RIESGOS Y MITIGACIÓN

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Tests rotos por cambios | Alto | CI/CD con validación en cada commit |
| Complejidad integraciones SOAP | Alto | Mocks para desarrollo, sandbox para testing |
| Burnout del equipo | Crítico | Sprints de 2 semanas, priorización clara |
| Deuda técnica acumulada | Medio | 20% del tiempo dedicado a refactorización |
| Cambios regulatorios | Alto | Arquitectura de adapters, monitoreo de RSS oficiales |

---

## 📈 MÉTRICAS DE ÉXITO

### Criterios de "100% Completado"

| Área | Meta | Medición |
|------|------|----------|
| **Cobertura de Tests** | >85% | JaCoCo report |
| **Bugs Críticos** | 0 | Issue tracker |
| **Latencia API** | <200ms (p95) | Prometheus |
| **Disponibilidad** | 99.9% | Uptime monitoring |
| **Transacciones/seg** | 100+ | Load testing |
| **Documentación** | 100% completa | Checklist de docs |
| **Seguridad** | OWASP Pass | ZAP scan |
| **Infraestructura** | Auto-scaling | K8s HPA active |

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS

### Día 1 (Hoy)
1. [Backend] Identificar y listar tests rotos
2. [QA] Configurar GitHub Actions para CI
3. [DevOps] Revisar configuración de k8s
4. [Frontend] Auditar componentes pendientes

### Día 2-3
1. [Backend] Reparar primeros 5 tests rotos
2. [QA] Implementar 3 tests E2E críticos
3. [DevOps] Configurar SonarQube en pipeline
4. [Frontend] Iniciar dashboard de reportes

### Día 4-5
1. [Backend] Reparar tests restantes + agregar nuevos
2. [QA] Validar flujo AFIP→ANMAT completo
3. [DevOps] Desplegar entorno de staging
4. [Frontend] Completar módulo de configuración

---

**Nota**: Este plan es dinámico y se ajustará según el progreso real y los impedimentos encontrados. Revisiones semanales para recalibrar prioridades.
