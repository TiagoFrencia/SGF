# 🚀 PLAN DE EJECUCIÓN: SGF al 100% - ESTADO REAL VERIFICADO

**Fecha de Actualización**: 2025-05-07  
**Equipo Requerido**: Backend Sr, Frontend Sr, QA Automation Sr, DevOps Sr  
**Duración Estimada Real**: 10-12 semanas  

---

## 📊 ESTADO ACTUAL VERIFICADO (Real vs Planificado)

### ✅ Completado Real (~60-65%)
| Fase | Estado Planificado | Estado Real | Brecha |
|------|-------------------|-------------|--------|
| **FASE 1**: Cimientos arquitectónicos | 100% | 100% ✅ | - |
| **FASE 2**: Módulos core operativos | 95% | 95% ✅ | - |
| **FASE 3**: Integraciones regulatorias | 90% | 90% ✅ | - |
| **FASE 4**: ETL de migración | 75% | 75% ⚠️ | Faltan tests |
| **FASE 5**: Interoperabilidad sanitaria | 50% | 50% ⚠️ | Faltan conectores |
| **FASE 6**: IA | 40% | 40% ⚠️ | Sin entrenamiento |
| **FASE 7**: Hardening | 15% | 15% 🔴 | Mayoría pendiente |

### 📈 Métricas Reales Verificadas
```
Archivos Java Main:     215 clases
Archivos Java Test:      11 tests (5.1% cobertura)
Servicios/Servidores:    33+ clases anotadas @Service/@Server
Módulos Frontend:        27 archivos TypeScript
Módulos Backend:         9 módulos Spring Boot
```

### 🔴 CRÍTICO: Cobertura de Tests
- **Actual**: 5.1% (11 tests para 215 clases = ~1 test cada 19 clases)
- **Objetivo**: 85%+ (requiere ~180 tests adicionales)
- **Déficit**: ~170 tests faltantes

---

## 🎯 OBJETIVOS POR FASE (Priorizados)

### **FASE 1: MASSIVE TEST COVERAGE (Semanas 1-3)** 🔴 PRIORIDAD CRÍTICA

#### Objetivo: Llevar cobertura de 5% → 70%

#### 1.1 Tests AFIP/Facturación Electrónica (15 tests)
- [ ] `WsaaSoapClientTest` - Token de acceso WSAA
- [ ] `CmsSignerTest` - Firma digital CMS
- [ ] `LoginTicketRequestBuilderTest` - Generación de tickets
- [ ] `AfipTokenServiceTest` - Gestión de tokens
- [ ] `WsfeSoapClientTest` - Comunicación WSFEv1
- [ ] `WsfeSoapEnvelopeBuilderTest` - Construcción SOAP
- [ ] `AfipServiceTest` - Servicio completo AFIP
- [ ] `AfipAuthorizationCommandTest` - Comando de autorización
- [ ] `AfipInvoiceTest` - Modelo de factura
- [ ] `AfipInvoiceTypeTest` - Tipos de factura (A, B, C, etc.)
- [ ] `AfipDocumentTypeTest` - Tipos de documento
- [ ] `AfipSandboxAuthorizationProviderTest` - Proveedor sandbox
- [ ] `AfipProductionAuthorizationProviderTest` - Proveedor producción
- [ ] `AfipConnectivityServiceTest` - Validación de conectividad
- [ ] `AfipControllerTest` - Endpoints REST

#### 1.2 Tests ANMAT/Trazabilidad (12 tests)
- [ ] `AnmatDataMatrixParserTest` - Parseo de DataMatrix GS1
- [ ] `AnmatDataMatrixTest` - Modelo DataMatrix
- [ ] `AnmatTraceabilityServiceTest` - Servicio de trazabilidad
- [ ] `AnmatTraceabilityGatewayTest` - Gateway ANMAT
- [ ] `AnmatTraceabilityEventTest` - Evento de trazabilidad
- [ ] `AnmatEventTypeTest` - Tipos de eventos
- [ ] `AnmatRemediationCaseTest` - Casos de remediación
- [ ] `TraceabilityReportCommandTest` - Comando de reportes
- [ ] `AnmatControllerTest` - Endpoints REST
- [ ] `AnmatHealthResponseTest` - Health check
- [ ] `AnmatTraceabilityInconsistencyTest` - Detección inconsistencias
- [ ] `AnmatRemediationActionTest` - Acciones de remediación

#### 1.3 Tests ADESFA/Obras Sociales (10 tests)
- [ ] `AdesfaServiceTest` - Servicio principal
- [ ] `AdesfaGatewayImplTest` - Implementación gateway
- [ ] `PamiValidatorTest` - Validador PAMI
- [ ] `OsdeValidatorTest` - Validador OSDE
- [ ] `SwissMedicalValidatorTest` - Validador Swiss Medical
- [ ] `AdesfaValidatorRegistryTest` - Registro de validadores
- [ ] `AdesfaValidationCommandTest` - Comando de validación
- [ ] `AdesfaValidationResultTest` - Resultado de validación
- [ ] `AdesfaControllerTest` - Endpoints REST
- [ ] `AdesfaModeTest` - Modos (sandbox/producción)

#### 1.4 Tests ETL/Migración (10 tests)
- [ ] `FarmaWinExtractorTest` - Extractor FarmaWin
- [ ] `NixfarmaExtractorTest` - Extractor Nixfarma
- [ ] `DbfExtractorTest` - Extractor DBF
- [ ] `DataTransformerTest` - Transformación de datos
- [ ] `DataValidatorTest` - Validación de datos
- [ ] `RollbackServiceTest` - Servicio de rollback
- [ ] `ShadowModeTest` - Modo sombra
- [ ] `MigrationDashboardTest` - Dashboard de migración
- [ ] `EtlMigrationRunTest` - Ejecución de migración
- [ ] `LegacyProductRecordTest` - Registro legacy

#### 1.5 Tests HL7 FHIR/Interoperabilidad (8 tests)
- [ ] `FhirServiceTest` - Servicio FHIR
- [ ] `FhirConfigTest` - Configuración FHIR
- [ ] `RecetaDigitalTest` - Receta digital
- [ ] `PacienteARTest` - Perfil Paciente Argentina
- [ ] `MedicamentoARTest` - Perfil Medicamento AR
- [ ] `CuirGeneratorTest` - Generador CUIR
- [ ] `RenapdisClientTest` - Cliente ReNaPDiS
- [ ] `ConsentimientoElectronicoTest` - Consentimiento

#### 1.6 Tests IA (15 tests) - 2 existentes, faltan 13
- [x] `ForecastingServiceTest` - ✅ Existente
- [x] `FraudDetectionServiceTest` - ✅ Existente
- [ ] `OnnxModelLoaderTest` - Carga de modelos ONNX
- [ ] `AnomalyDetectorTest` - Detección de anomalías
- [ ] `ShapExplainerTest` - Explicabilidad SHAP
- [ ] `ForecastingIntegrationTest` - Integración forecasting
- [ ] `FraudDetectionIntegrationTest` - Integración fraude
- [ ] `ModelTrainingPipelineTest` - Pipeline de entrenamiento
- [ ] `OcrRecipeServiceTest` - OCR de recetas
- [ ] `TesseractIntegrationTest` - Integración Tesseract
- [ ] `AlertNotificationServiceTest` - Notificaciones de alertas
- [ ] `SubstitutionSuggestionTest` - Sugerencias de sustitución
- [ ] `StockOptimizationTest` - Optimización de stock
- [ ] `DemandPatternAnalysisTest` - Análisis de patrones
- [ ] `PriceElasticityTest` - Elasticidad de precios

#### 1.7 Tests Core/Negocio (25 tests)
- [x] `BaseEntityTest` - ✅ Existente
- [x] `EventBusTest` - ✅ Existente
- [ ] `AggregateRootTest` - Raíz de agregado
- [ ] `DomainEventTest` - Evento de dominio
- [ ] `SpecificationTest` - Especificaciones
- [ ] `AuditServiceTest` - ✅ Parcialmente existente
- [ ] `JwtServiceTest` - Servicio JWT
- [ ] `AuthServiceTest` - Servicio de autenticación
- [ ] `SgfUserDetailsServiceTest` - Detalles de usuario
- [ ] `JwtAuthenticationFilterTest` - Filtro JWT
- [ ] `PermissionEvaluatorTest` - Evaluador de permisos
- [ ] `RoleHierarchyTest` - Jerarquía de roles
- [ ] `PasswordEncryptionTest` - Encriptación de contraseñas
- [ ] `SessionManagementTest` - Gestión de sesiones
- [ ] `MultiTenantContextTest` - Contexto multi-tenant
- [ ] `BranchConfigurationTest` - Configuración de sucursales
- [ ] `UserPreferenceTest` - Preferencias de usuario
- [ ] `NotificationTemplateTest` - Plantillas de notificación
- [ ] `EmailServiceTest` - Servicio de email
- [ ] `SmsServiceTest` - Servicio de SMS
- [ ] `PushNotificationTest` - Notificaciones push
- [ ] `WebhookDispatcherTest` - Dispatcher de webhooks
- [ ] `ApiRateLimiterTest` - Limitador de tasa API
- [ ] `CacheManagerTest` - Gestor de caché
- [ ] `HealthIndicatorTest` - Indicadores de salud

#### 1.8 Tests Inventory/Stock (15 tests)
- [ ] `InventoryServiceTest` - ✅ Parcialmente existente
- [ ] `BranchTransferServiceTest` - Transferencias entre sucursales
- [ ] `ReorderPointServiceTest` - Puntos de reposición
- [ ] `ExpiryAlertServiceTest` - Alertas de vencimiento
- [ ] `StockAdjustmentTest` - Ajustes de stock
- [ ] `InventoryCountTest` - Conteo de inventario
- [ ] `StockMovementTest` - Movimientos de stock
- [ ] `BatchTrackingTest` - Seguimiento de lotes
- [ ] `SerialNumberTrackingTest` - Números de serie
- [ ] `WarehouseLocationTest` - Ubicaciones de almacén
- [ ] `StockReservationTest` - Reservas de stock
- [ ] `InventorySnapshotTest` - Instantáneas de inventario
- [ ] `StockValuationTest` - Valuación de stock
- [ ] `DeadStockAnalysisTest` - Análisis de stock muerto
- [ ] `TurnoverRatioTest` - Ratio de rotación

#### 1.9 Tests POS/Ventas (15 tests)
- [ ] `SalesServiceTest` - ✅ Parcialmente existente
- [ ] `PosOrderServiceTest` - Servicio de órdenes POS
- [ ] `MultiOrderServiceTest` - Múltiples órdenes
- [ ] `HotkeyServiceTest` - Servicio de atajos
- [ ] `BarcodeServiceTest` - Servicio de códigos de barra
- [ ] `ObraSocialDiscountServiceTest` - Descuentos obras sociales
- [ ] `CartManagementTest` - Gestión de carrito
- [ ] `PaymentProcessingTest` - Procesamiento de pagos
- [ ] `ReceiptGenerationTest` - Generación de recibos
- [ ] `CustomerLoyaltyTest` - Lealtad de clientes
- [ ] `PromotionEngineTest` - Motor de promociones
- [ ] `TaxCalculationTest` - Cálculo de impuestos
- [ ] `ReturnProcessingTest` - Procesamiento de devoluciones
- [ ] `QuoteManagementTest` - Gestión de presupuestos
- [ ] `OfflineSyncTest` - Sincronización offline

#### 1.10 Tests Catalog/Productos (12 tests)
- [ ] `ProductServiceTest` - ✅ Parcialmente existente
- [ ] `CategoryManagementTest` - Gestión de categorías
- [ ] `BrandManagementTest` - Gestión de marcas
- [ ] `ProductVariantTest` - Variantes de productos
- [ ] `PricingStrategyTest` - Estrategias de precios
- [ ] `DiscountRuleTest` - Reglas de descuento
- [ ] `ProductImageTest` - Imágenes de productos
- [ ] `ProductAttributeTest` - Atributos de productos
- [ ] `BarcodeLookupTest` - Búsqueda por código de barras
- [ ] `DrugInteractionTest` - Interacciones medicamentosas
- [ ] `TherapeuticClassTest` - Clases terapéuticas
- [ ] `ActiveIngredientTest` - Principios activos

#### 1.11 Tests Sync/Offline (10 tests)
- [ ] `LocalCommandHandlerTest` - ✅ Parcialmente existente
- [ ] `LastWriteWinsResolverTest` - ✅ Parcialmente existente
- [ ] `ConflictResolutionStrategyTest` - Estrategias de conflicto
- [ ] `OfflineQueueManagerTest` - Gestor de cola offline
- [ ] `SyncSchedulerTest` - Programador de sincronización
- [ ] `DeltaSyncTest` - Sincronización delta
- [ ] `FullSyncTest` - Sincronización completa
- [ ] `NetworkConnectivityTest` - Conectividad de red
- [ ] `DataCompressionTest` - Compresión de datos
- [ ] `EncryptionAtRestTest` - Encriptación en reposo

#### 1.12 Tests Integration/E2E (20 tests)
- [ ] `OutboxServiceTest` - ✅ Parcialmente existente
- [ ] `AfipToAnmatFlowTest` - Flujo AFIP → ANMAT
- [ ] `CompleteSaleFlowTest` - Flujo completo de venta
- [ ] `OfflineFirstFlowTest` - Flujo offline-first
- [ ] `MultiBranchSyncTest` - Sincronización multi-sucursal
- [ ] `UserAuthenticationFlowTest` - Flujo de autenticación
- [ ] `InventoryAdjustmentFlowTest` - Flujo de ajuste de inventario
- [ ] `PrescriptionValidationFlowTest` - Flujo de validación de recetas
- [ ] `InsuranceClaimFlowTest` - Flujo de reclamos de seguros
- [ ] `ReportingPipelineTest` - Pipeline de reportes
- [ ] `AuditTrailTest` - Trail de auditoría
- [ ] `NotificationDeliveryTest` - Entrega de notificaciones
- [ ] `BackupRecoveryTest` - Backup y recuperación
- [ ] `DataMigrationFlowTest` - Flujo de migración de datos
- [ ] `ApiVersioningTest` - Versionado de API
- [ ] `LoadBalancingTest` - Balanceo de carga
- [ ] `FailoverTest` - Conmutación por error
- [ ] `SecurityHeadersTest` - Cabeceras de seguridad
- [ ] `CorsPolicyTest` - Política CORS
- [ ] `RateLimitingTest` - Limitación de tasa

---

### **FASE 2: INTEROPERABILIDAD SANITARIA (Semanas 4-6)**

#### 2.1 Conectores Pendientes
- [ ] **PAMI SIAFAR**: Cliente SOAP para validación de recetas
- [ ] **PAMI ValidaCOFA**: Validación de COFA de medicamentos
- [ ] **REFEPS**: Validación de matrículas profesionales
- [ ] **ReNaPDiS Completo**: Módulo psicofármacos

#### 2.2 Recetas Digitales
- [ ] Firma digital con certificados digitales
- [ ] Almacenamiento seguro con auditoría
- [ ] API REST para gestión de recetas
- [ ] QR para validación de recetas

#### 2.3 Consentimiento Electrónico
- [ ] Modelo de dominio completo
- [ ] Endpoints REST
- [ ] Auditoría de accesos
- [ ] Plantillas personalizables

---

### **FASE 3: IA OPERATIVA (Semanas 7-8)**

#### 3.1 Entrenamiento de Modelos
- [ ] Dataset histórico simulado (10,000+ transacciones)
- [ ] Entrenamiento modelo forecasting
- [ ] Entrenamiento modelo detección de fraude
- [ ] Validación cruzada de modelos

#### 3.2 Producción IA
- [ ] Modelo ONNX en producción
- [ ] Endpoint de predicciones
- [ ] Dashboard de forecasting
- [ ] Alertas de fraude en tiempo real

#### 3.3 OCR Recetas MVP
- [ ] Integración Tesseract
- [ ] Pre-procesamiento de imágenes
- [ ] Extracción de campos clave
- [ ] Confirmación humana para baja confianza

---

### **FASE 4: HARDENING EMPRESARIAL (Semanas 9-12)**

#### 4.1 Seguridad
- [ ] OWASP Top 10 scan con ZAP
- [ ] Encriptación de datos sensibles
- [ ] Logs inmutables
- [ ] Pentesting externo

#### 4.2 Performance
- [ ] HPA en Kubernetes
- [ ] Redis para caché
- [ ] Optimización de consultas
- [ ] Load testing (100+ transacciones/seg)

#### 4.3 CI/CD
- [ ] Deploy automatizado
- [ ] Blue-green deployments
- [ ] Rollback automático
- [ ] Canary releases

#### 4.4 Documentación
- [ ] OpenAPI/Swagger completo
- [ ] Guías de despliegue
- [ ] Manual técnico
- [ ] Runbooks de operaciones

---

## 🛠️ SECRETS REQUERIDOS PARA CI/CD

```yaml
# GitHub Secrets necesarios
SONAR_TOKEN: "tu_token_sonarqube"
SONAR_HOST_URL: "https://sonarqube.example.com"
AFIP_CUIT: "30123456789"
AFIP_CERTIFICATE: "base64_encoded_cert"
AFIP_PRIVATE_KEY: "base64_encoded_key"
ANMAT_API_KEY: "tu_api_key_anmat"
ADESFA_API_KEY: "tu_api_key_adesfa"
DOCKER_USERNAME: "usuario_docker"
DOCKER_PASSWORD: "password_docker"
K8S_KUBECONFIG: "base64_encoded_kubeconfig"
```

---

## 📅 CRONOGRAMA DETALLADO

### Semana 1-2: Tests AFIP + ANMAT + ADESFA
- **Día 1-3**: Tests AFIP (15 tests)
- **Día 4-6**: Tests ANMAT (12 tests)
- **Día 7-10**: Tests ADESFA (10 tests)
- **Meta**: 37 tests nuevos, cobertura ~25%

### Semana 3: Tests ETL + FHIR + Core
- **Día 1-3**: Tests ETL (10 tests)
- **Día 4-5**: Tests FHIR (8 tests)
- **Día 6-10**: Tests Core (25 tests)
- **Meta**: 43 tests nuevos, cobertura ~45%

### Semana 4: Tests Inventory + POS + Catalog
- **Día 1-4**: Tests Inventory (15 tests)
- **Día 5-7**: Tests POS (15 tests)
- **Día 8-10**: Tests Catalog (12 tests)
- **Meta**: 42 tests nuevos, cobertura ~65%

### Semana 5: Tests Sync + Integration + E2E
- **Día 1-3**: Tests Sync (10 tests)
- **Día 4-10**: Tests Integration/E2E (20 tests)
- **Meta**: 30 tests nuevos, cobertura ~80%

### Semana 6: Interoperabilidad Sanitaria
- **Día 1-3**: Conector PAMI SIAFAR
- **Día 4-5**: Conector REFEPS
- **Día 6-8**: ReNaPDiS completo
- **Día 9-10**: Recetas digitales
- **Meta**: FASE 5 al 100%

### Semana 7-8: IA Operativa
- **Semana 7**: Entrenamiento de modelos
- **Semana 8**: Producción IA + OCR MVP
- **Meta**: FASE 6 al 100%

### Semana 9-10: Seguridad + Performance
- **Semana 9**: OWASP + Encriptación
- **Semana 10**: HPA + Redis + Optimización
- **Meta**: FASE 7 al 80%

### Semana 11-12: CI/CD + Documentación
- **Semana 11**: CI/CD avanzado
- **Semana 12**: Documentación + Pulido final
- **Meta**: PROYECTO 100% ✅

---

## ✅ CHECKLIST FINAL "PROYECTO 100%"

### Código
- [ ] 0 tests rotos
- [ ] Cobertura >85%
- [ ] 0 bugs críticos
- [ ] Code smell <5%
- [ ] Duplicación <3%

### Infraestructura
- [ ] Kubernetes HPA activo
- [ ] Redis configurado
- [ ] Prometheus + Grafana
- [ ] ELK Stack para logs
- [ ] Backups automáticos

### Seguridad
- [ ] OWASP Top 10 pass
- [ ] Encriptación TLS 1.3
- [ ] Datos sensibles encriptados
- [ ] Logs inmutables
- [ ] Audit trail completo

### Performance
- [ ] Latencia API <200ms (p95)
- [ ] 100+ transacciones/segundo
- [ ] Disponibilidad 99.9%
- [ ] Recovery time <5min

### Documentación
- [ ] OpenAPI completo
- [ ] Guías de despliegue
- [ ] Manual técnico
- [ ] Runbooks operaciones
- [ ] README actualizado

---

**Nota**: Este plan se actualizará semanalmente según el progreso real. Revisiones cada viernes para recalibrar prioridades.
