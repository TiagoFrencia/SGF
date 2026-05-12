# Roadmap SGF

Documento estrategico de mediano y largo plazo.

**Importante:** este roadmap no describe por si solo el estado ejecutado del repo. Para el estado real auditado usar [[MASTER_PLAN]]. Para el bloqueo tecnico actual y los proximos pasos operativos usar tambien [[00_DASHBOARD]].

---

## Objetivo

Evolucionar SGF desde su base actual de monolito modular enterprise-ready hacia una plataforma farmaceutica mas interoperable, endurecida y comercializable para el mercado argentino.

## Foto de partida

Al momento de esta revision, SGF ya cuenta con:

- backend modular Spring Boot
- frontend Angular 17
- inventario, POS, auditoria y sync base
- AFIP, ANMAT y ADESFA iniciados con bastante profundidad
- catalogo/Vademecum publico CNPM/MSal ya incorporado como primera fuente real de medicamentos para prototipo
- ETL, OCR y AI operativa en estado base/parcial
- infraestructura K8s y monitoreo base

Las fases siguientes deben leerse como **expansion, cierre productivo y endurecimiento**, no como un proyecto arrancando desde cero.

---

## Principios del roadmap

1. **Mantener modular monolith first** mientras el costo de coordinacion de microservicios no se justifique.
2. **Cerrar gaps productivos** antes de agregar mas superficie aspiracional.
3. **Priorizar integracion regulatoria real** por encima de features cosmeticas.
4. **Construir sobre evidencia de operacion**: tests, observabilidad y runbooks.
5. **Separar claramente base tecnica vs capacidad productiva validada**.

---

## Fases estrategicas

### Fase A. Cierre operativo del baseline

**Meta:** convertir la base actual en una plataforma internamente consistente y mas verificable.

#### Prioridades

- ampliar cobertura E2E del circuito POS -> AFIP/ANMAT/ADESFA
- consolidar documentacion operativa y runbooks
- reducir drift entre codigo y documentacion
- cerrar el baseline del backend antes de mas expansion funcional

#### Estado actual

`En progreso avanzado`

La base existe y avanzo fuerte en harness, migraciones y wiring comun, pero todavia falta cerrar por completo `integrationTest`.

### Fase B. Interoperabilidad sanitaria real

**Meta:** pasar de contratos y mocks a conectividad sanitaria productiva.

#### Prioridades

- implementar cliente real de PAMI SIAFAR
- implementar integracion real de REFEPS
- ampliar soporte FHIR y perfiles nacionales ya iniciados
- endurecer los flujos de venta con validacion sanitaria completa
- usar los codigos SNOMED ya incorporados al catalogo como base terminologica, sin confundirlos con conectividad sanitaria productiva

#### Estado actual

`En progreso desde base/mock`

Ya existen contratos, DTOs, wiring parcial en ventas y base SNOMED desde CNPM/MSal, pero no esta cerrado el tramo productivo de PAMI/REFEPS.

### Fase C. ETL y migracion productiva

**Meta:** transformar el stack ETL ya iniciado en una herramienta de migracion utilizable con confianza.

#### Prioridades

- convertir la etapa `load` en persistencia real
- completar rollback operativo
- cerrar dashboard y validacion post-migracion
- documentar mapeos legacy -> SGF
- mantener separado el importador CNPM/MSal ya real de la carga general de sistemas legados

#### Estado actual

`En progreso`

La extraccion, transformacion y validacion ya existen. La carga real especifica de Vademecum CNPM/MSal ya existe; falta cerrar la carga legacy general y la operacion completa.

### Fase D. AI productiva

**Meta:** pasar de capacidades AI presentes a serving operacional y medible.

#### Prioridades

- empaquetar modelos ONNX reales
- medir fallback, calidad y tiempos de inferencia
- ampliar OCR con datasets y validacion real
- conectar recomendaciones AI con decisiones operativas auditables

#### Estado actual

`Base implementada`

Forecasting, fraude y OCR ya existen; falta cierre de serving y empaquetado productivo.

### Fase E. Hardening empresarial y comercial

**Meta:** llevar SGF desde una base tecnica fuerte hacia operacion enterprise sostenible.

#### Prioridades

- hardening de seguridad en K8s y plataforma
- pruebas de carga y resiliencia
- estrategia multi-tenant endurecida
- runbooks, soporte, onboarding y readiness comercial

#### Estado actual

`Base iniciada`

Ya hay manifests, monitoreo y load test base, pero no un hardening enterprise completo.

---

## Hitos deseados

### Corto plazo

- baseline backend completamente verde
- Vademecum publico CNPM/MSal estabilizado como dataset de desarrollo/prototipo
- PAMI SIAFAR real
- REFEPS real
- ETL legacy `load` real
- modelo ONNX empaquetado
- mas pruebas E2E

### Mediano plazo

- integracion sanitaria operativa extremo a extremo
- dashboards de operacion mas maduros
- despliegues mas endurecidos

### Largo plazo

- producto comercializable con procesos de soporte, operacion y evolucion continua

---

## Riesgos principales

| Riesgo | Impacto | Mitigacion |
|--------|---------|------------|
| Confundir base tecnica con capacidad productiva | Alto | Documentar estados reales y exigir validacion operativa |
| Integraciones sanitarias y regulatorias incompletas | Alto | Priorizar cierres reales antes de expandir superficie |
| Confundir CNPM/MSal publico con SLA comercial | Medio | Documentar que AlfaBeta/Kairos siguen siendo camino pago/productivo |
| Drift entre documentacion y repo | Medio | Auditorias documentales periodicas |
| Hardening de plataforma insuficiente | Alto | Roadmap explicito de seguridad y resiliencia |
| Exceso de alcance simultaneo | Critico | Avanzar por cierres verticales verificables |

---

## Relacion con otros documentos

- [[MASTER_PLAN]]: estado operativo real, brechas y proximos pasos verificables
- [[00_DASHBOARD]]: portada ejecutiva y bloqueo principal actual
- [[ARCHITECTURE]]: arquitectura efectiva del repo
- [[README]]: onboarding tecnico y contrato operativo
- [[Archive/|Archive]]: historico documental
