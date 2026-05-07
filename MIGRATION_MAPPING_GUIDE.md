# 📖 Guía de Mapeo: Sistemas Legacy → SGF

## Propósito

Este documento describe el mapeo detallado de esquemas de datos desde los sistemas legacy más comunes (FarmaWin, Nixfarma, DBF Genérico) hacia el esquema unificado del Sistema de Gestión Farmacéutica (SGF).

---

## 1. FarmaWin → SGF

### Tabla: ARTICULOS

| Campo FarmaWin | Tipo | Campo SGF | Transformación | Notas |
|----------------|------|-----------|----------------|-------|
| `ART_ID` | VARCHAR(20) | `legacy_id` | Directo | ID único del sistema legacy |
| `ART_EAN` | VARCHAR(13) | `gtin` | Pad a 14 dígitos | Agregar leading zero si tiene 13 dígitos |
| `ART_NOMBRE` | VARCHAR(200) | `commercial_name` | Directo | Nombre comercial completo |
| `ART_LABORATORIO` | VARCHAR(100) | `brand` | Directo | Laboratorio fabricante |
| `ART_FORMA` | VARCHAR(50) | `pharmaceutical_form` | Normalizar | Ver tabla de normalización abajo |
| `ART_CONCENTRACION` | VARCHAR(50) | `concentration` | Directo | Ej: "500mg", "1%" |
| `ART_STOCK` | INTEGER | `current_stock` | Directo | Stock actual |
| `ART_COSTO` | DECIMAL(12,4) | `unit_cost` | Redondear a 2 decimales | Costo unitario |
| `ART_PRECIO_VTA` | DECIMAL(12,4) | `retail_price` | Redondear a 2 decimales | Precio de venta |
| `ART_LOTE` | VARCHAR(50) | `lot_number` | Directo | Número de lote |
| `ART_VENCIMIENTO` | DATE (dd/MM/yyyy) | `expiry_date` | Parsear formato | Convertir a ISO yyyy-MM-dd |
| `ART_PROVEEDOR_CUIT` | VARCHAR(15) | `supplier_cuit` | Formatear XX-XXXXXXXX-X | Validar checksum |
| `ART_RECETA` | CHAR(1) S/N | `prescription_required` | Boolean | 'S' → true |
| `ART_ANMAT_CAT` | VARCHAR(50) | `anmat_category` | Directo | Categoría ANMAT si existe |

### Tabla: STOCK (Lotes)

| Campo FarmaWin | Tipo | Campo SGF | Transformación |
|----------------|------|-----------|----------------|
| `STK_ART_ID` | VARCHAR(20) | `legacy_id` | FK a ARTICULOS |
| `STK_LOTE` | VARCHAR(50) | `lot_number` | Directo |
| `STK_CANTIDAD` | INTEGER | `current_stock` | Sumar por producto |
| `STK_VENCIMIENTO` | DATE | `expiry_date` | Formato dd/MM/yyyy |

### Normalización de Formas Farmacéuticas (FarmaWin)

| Código Legacy | Estándar SGF |
|---------------|--------------|
| COMP, COM., COMPRIMIDO | COMPRIMIDOS |
| CAPS, CAP., CAPSULA | CAPSULAS |
| JAR, JBE, JARABE | JARABE |
| INY, AMP, AMPOLLA | INYECTABLE |
| SUSP, SUSPENSION | SUSPENSION |
| CREM, CREMA | CREMA |
| UNG, UNGUENTO | UNGUENTO |
| SOL, SOLUCION | SOLUCION |
| GOT, GOTAS | GOTAS |
| OV, OVULOS | OVULOS |
| SUP, SUPOSITORIOS | SUPOSITORIOS |
| POL, POLVO | POLVO |
| INH, AER, INHALADOR | INHALADOR |

---

## 2. Nixfarma → SGF

### Tabla: nf_productos

| Campo Nixfarma | Tipo | Campo SGF | Transformación | Notas |
|----------------|------|-----------|----------------|-------|
| `product_code` | VARCHAR(20) | `legacy_id` | Directo | Formato: NXF-000001 |
| `gtin` | BIGINT/VARCHAR | `gtin` | Pad a 14 dígitos | Puede perder leading zeros |
| `product_name` | VARCHAR(200) | `commercial_name` | Directo | Nombre completo |
| `active_ingredient` | VARCHAR(100) | `active_ingredient` | Directo | Principio activo |
| `concentration` | VARCHAR(50) | `concentration` | Directo | Concentración |
| `form` | VARCHAR(50) | `pharmaceutical_form` | Normalizar | Ver tabla abajo |
| `current_stock` | INTEGER | `current_stock` | Directo | Stock total |
| `unit_cost` | DECIMAL(10,2) | `unit_cost` | Directo | Costo unitario |
| `retail_price` | DECIMAL(10,2) | `retail_price` | Directo | Precio venta |
| `lot_number` | VARCHAR(50) | `lot_number` | Directo | Lote |
| `expiry_date` | DATE (yyyy-MM-dd) | `expiry_date` | Directo | Formato ISO |
| `brand` | VARCHAR(100) | `brand` | Directo | Laboratorio |
| `supplier_cuit` | VARCHAR(15) | `supplier_cuit` | Validar formato | XX-XXXXXXXX-X |
| `prescription_required` | CHAR(1) S/N | `prescription_required` | Boolean | 'S' → true |
| `therapeutic_category` | VARCHAR(100) | `therapeutic_category` | Directo | Categoría terapéutica |

### Tabla: nf_lotes

| Campo Nixfarma | Tipo | Campo SGF | Transformación |
|----------------|------|-----------|----------------|
| `lote_producto_id` | INTEGER | `legacy_id` | FK a nf_productos |
| `lote_numero` | VARCHAR(50) | `lot_number` | Directo |
| `lote_sucursal_id` | INTEGER | - | Ignorar (se consolida) |
| `lote_cantidad` | INTEGER | `current_stock` | Sumar por producto |
| `lote_vencimiento` | DATE | `expiry_date` | yyyy-MM-dd |

### Normalización de Formas Farmacéuticas (Nixfarma)

| Código Legacy | Estándar SGF |
|---------------|--------------|
| COM, COMP, TAB, TABLETA | COMPRIMIDOS |
| CAP, CAPS | CAPSULAS |
| JBE, JAR, JARABE | JARABE |
| INY, AMP, AMPOLLA | INYECTABLE |
| SUSP | SUSPENSION |
| CREMA | CREMA |
| UNG | UNGUENTO |
| SOL | SOLUCION |
| GOTAS, GTS | GOTAS |
| OV | OVULOS |
| SUP | SUPOSITORIOS |
| POLVO | POLVO |
| INH, AER | INHALADOR |
| GEL | GEL |

---

## 3. DBF Genérico → SGF

### Estructura Típica DBF/dBase III/IV

| Campo DBF | Tipo | Campo SGF | Transformación | Notas |
|-----------|------|-----------|----------------|-------|
| `CODIGO` / `ARTICULO` | Character(20) | `legacy_id` | Directo | Variable según instalación |
| `EAN` / `GTIN` / `BARRAS` | Numeric/Character | `gtin` | Pad a 14 dígitos | Numeric pierde ceros |
| `NOMBRE` / `DESCRIPCION` | Character(200) | `commercial_name` | Directo | Puede incluir IFA |
| `IFA` / `ACTIVO` / `MONODROGA` | Character(100) | `active_ingredient` | Directo | Principio activo |
| `CONCENTRACION` / `DOSIS` | Character(50) | `concentration` | Directo | Con o sin unidad |
| `FORMA` / `TIPO` | Character(50) | `pharmaceutical_form` | Normalizar | Códigos variables |
| `STOCK` / `CANTIDAD` | Numeric | `current_stock` | Directo | Entero |
| `COSTO` / `COMPRA` | Numeric | `unit_cost` | Dividir por 100 si implícito | 2 decimales implícitos comunes |
| `PRECIO` / `VENTA` | Numeric | `retail_price` | Dividir por 100 si implícito | 2 decimales implícitos |
| `LOTE` | Character(50) | `lot_number` | Directo | |
| `VTO` / `VENCIMIENTO` | Date (YYYYMMDD) | `expiry_date` | Parsear formato | 8 caracteres |
| `CUIT` / `PROVEEDOR` | Character(15) | `supplier_cuit` | Formatear | XX-XXXXXXXX-X |
| `RECETA` / `RX` | Logical/Character | `prescription_required` | Boolean | T/F, S/N, Y/N |

### Auto-Detección de Columnas

El extractor DBF utiliza auto-detección basada en patrones de nombres:

| Patrón detectado | Mapea a | Ejemplos |
|------------------|---------|----------|
| `codigo`, `código`, `id`, `articulo` | `id` | COD_ART, ART_ID, PRODUCTO_COD |
| `gtin`, `ean`, `cod_barra`, `barras` | `gtin` | EAN13, COD_BARRA, BARRAS |
| `nombre`, `descripcion`, `producto` | `name` | DESC_COMPLETA, NOMB_COMERCIAL |
| `principio`, `activo`, `ifa`, `dci` | `active` | MONODROGA, PRINCIPIO_ACTIVO |
| `concent`, `dosis`, `mg_` | `concentration` | CONCENTRACION, DOSIS_MG |
| `forma`, `form`, `presentacion` | `form` | FORMA_FARM, TIPO_PRODUCTO |
| `stock`, `cant`, `cantidad`, `exist` | `stock` | EXISTENCIA, CANT_DISP |
| `costo`, `compra`, `precio_compra` | `cost` | ULT_COSTO, COSTO_UNIT |
| `precio`, `venta`, `pvp`, `retail` | `price` | PVP, PRECIO_VENTA |
| `lote`, `nro_lote`, `batch` | `lot` | NUMERO_LOTE, BATCH_ID |
| `vto`, `venc`, `fecha_venc`, `cad` | `expiry` | VTO_DE, CADUCIDAD |
| `marca`, `laboratorio`, `lab` | `brand` | LAB_FABRICANTE, MARCA |
| `cuit`, `proveedor` | `cuit` | CUIT_PROV, PROVEEDOR_CUIT |
| `receta`, `presc`, `rx` | `rx` | VENTA_RX, RECETA_OBLIG |

### Codificación de Caracteres

| Sistema | Encoding común | Notas |
|---------|----------------|-------|
| dBase III | CP437 | Inglés, limitado |
| dBase IV | CP850 | Español con ñ y acentos |
| Clipper | CP850 | Muy común en Argentina |
| Visual FoxPro | CP1252 | Windows Latin-1 |

**Recomendación**: Intentar CP850 primero, fallback a UTF-8 o CP1252.

---

## 4. Reglas de Transformación Comunes

### GTIN (Global Trade Item Number)

```java
// Padding a 14 dígitos (GTIN-14)
String padded = String.format("%014d", Long.parseLong(gtin));

// Validación de checksum (algoritmo GS1)
int sum = 0;
for (int i = 0; i < 13; i++) {
    int digit = Character.getNumericValue(gtin.charAt(i));
    sum += (i % 2 == 0) ? digit : digit * 3;
}
int checkDigit = (10 - (sum % 10)) % 10;
```

### CUIT (Clave Única de Identificación Tributaria)

```java
// Formato: XX-XXXXXXXX-X
String formatted = cuit.substring(0, 2) + "-" + 
                   cuit.substring(2, 10) + "-" + 
                   cuit.substring(10, 11);

// Validación checksum
int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
int sum = 0;
for (int i = 0; i < 10; i++) {
    sum += digits[i] * multipliers[i];
}
int remainder = sum % 11;
int checkDigit = remainder == 0 ? 0 : remainder == 1 ? -1 : 11 - remainder;
// Caso especial: si checkDigit == -1, depende del prefix (30→9, 23/24/27→4)
```

### Fechas

| Sistema Legacy | Formato | Conversión |
|----------------|---------|------------|
| FarmaWin | dd/MM/yyyy | `DateTimeFormatter.ofPattern("dd/MM/yyyy")` |
| Nixfarma | yyyy-MM-dd | `LocalDate.parse()` (ISO nativo) |
| DBF | YYYYMMDD | `DateTimeFormatter.ofPattern("yyyyMMdd")` |

### Precios con Decimales Implícitos

```java
// DBF común: 450000 significa $4500.00
BigDecimal cost = BigDecimal.valueOf(rawValue, 2); // 2 decimales implícitos

// Redondeo estándar a 2 decimales
BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
```

---

## 5. Calidad de Datos y Validaciones

### Validaciones Críticas (Blocking)

| Regla | Mensaje de Error | Acción |
|-------|------------------|--------|
| GTIN vacío/null | "GTIN vacío — no se puede importar" | Rechazar registro |
| GTIN longitud inválida | "GTIN debe tener 13-14 dígitos" | Rechazar registro |
| Nombre comercial vacío | "Nombre comercial vacío" | Rechazar registro |
| Forma farmacéutica vacía | "Forma farmacéutica no especificada" | Rechazar registro |
| Vencimiento > 5 años pasado | "Vencido hace más de 5 años" | Rechazar registro |
| Stock negativo | "Stock negativo" | Rechazar registro |

### Advertencias (Non-Blocking)

| Regla | Mensaje | Acción Recomendada |
|-------|---------|-------------------|
| Nombre > 200 caracteres | "Será truncado" | Revisar en sistema origen |
| Principio activo vacío | "Marcar para revisión manual" | Completar post-migración |
| Concentración vacía | "No especificada" | Completar con vademécum |
| Producto vencido | "Vencido: fecha" | Separar para descarte |
| Costo cero/negativo | "Revisar precio" | Actualizar con lista vigente |
| CUIT inválido | "Checksum fallido" | Verificar con proveedor |
| IFA con números | "Sospechoso" | Revisar carga original |

---

## 6. Procedimiento de Migración

### Paso 1: Shadow Mode (Pre-Migración)

```bash
# Ejecutar shadow mode para evaluar calidad de datos
curl -X POST http://localhost:8080/api/etl/shadow/run?source=FarmaWin

# Review del reporte
{
  "readiness": "READY",
  "overallScore": 92.5,
  "totalRecords": 5000,
  "passed": 4625,
  "failed": 375,
  "recommendation": "✓ Listo para migración. Datos de alta calidad."
}
```

### Paso 2: Dry-Run Migration

```bash
# Ejecutar migración en modo dry-run (valida pero no escribe)
curl -X POST http://localhost:8080/api/etl/migration/start \
  -d '{"sourceSystem":"FarmaWin","connectionString":"jdbc:...","dryRun":true}'
```

### Paso 3: Migración Real

```bash
# Iniciar migración real
POST /api/etl/migration/start
{
  "sourceSystem": "FarmaWin",
  "connectionString": "jdbc:firebird:localhost/db.fdb",
  "dryRun": false
}

# Respuesta: {"migrationId":"FarmaWin-a1b2c3d4"}

# Monitorear progreso
GET /api/etl/migration/FarmaWin-a1b2c3d4/progress

# Dashboard snapshot
{
  "status": "RUNNING",
  "percent": 45,
  "extracted": 2250,
  "passed": 2100,
  "failed": 150,
  "loaded": 2100
}
```

### Paso 4: Validación Post-Migración

```bash
# Obtener reporte final
GET /api/etl/migration/FarmaWin-a1b2c3d4/dashboard

# Verificar métricas
- Total records migrated: X
- Pass rate: Y%
- Failed records: Z (exportable para revisión)
```

### Paso 5: Cleanup o Rollback

```bash
# Si todo está correcto, limpiar plan de rollback
DELETE /api/etl/rollback/FarmaWin-a1b2c3d4

# Si hay problemas, ejecutar rollback
POST /api/etl/rollback/FarmaWin-a1b2c3d4/execute
```

---

## 7. Apéndice: Ejemplos de Datos

### Ejemplo FarmaWin

```sql
-- ARTICULOS
INSERT INTO ARTICULOS VALUES (
  '0001',                    -- ART_ID
  '7791234000010',           -- ART_EAN
  'IBUPROFENO 600mg x 20',   -- ART_NOMBRE
  'BAGO',                    -- ART_LABORATORIO
  'COMP',                    -- ART_FORMA
  '600mg',                   -- ART_CONCENTRACION
  150,                       -- ART_STOCK
  2500.0000,                 -- ART_COSTO
  3750.0000,                 -- ART_PRECIO_VTA
  'LOTE-A001',               -- ART_LOTE
  '31/12/2025',              -- ART_VENCIMIENTO
  '30-12345678-9',           -- ART_PROVEEDOR_CUIT
  'N',                       -- ART_RECETA
  'BAJO_RIESGO'              -- ART_ANMAT_CAT
);
```

### Ejemplo Nixfarma

```sql
-- nf_productos
INSERT INTO nf_productos VALUES (
  'NXF-00001',               -- product_code
  7791234000010,             -- gtin (BIGINT)
  'OMEPRAZOL 20mg x 28',     -- product_name
  'OMEPRAZOL',               -- active_ingredient
  '20mg',                    -- concentration
  'COM',                     -- form
  200,                       -- current_stock
  2800.00,                   -- unit_cost
  4200.00,                   -- retail_price
  'LOTE-N01',                -- lot_number
  '2026-03-15',              -- expiry_date
  'BAGO',                    -- brand
  '30-12345678-9',           -- supplier_cuit
  'S',                       -- prescription_required
  'ANTIULCEROSO'             -- therapeutic_category
);
```

### Ejemplo DBF

```
CODIGO,EAN,NOMBRE,IFA,CONCENTRACION,FORMA,STOCK,COSTO,PRECIO,LOTE,VTO,CUIT,RX
00001,7791234000010,"IBUPROFENO 400mg x 20",IBUPROFENO,400mg,COM,100,350000,525000,A001,20251231,30123456789,N
```

---

## 8. Contacto y Soporte

Para consultas sobre mapeos específicos no cubiertos en esta guía:

- **Email**: soporte@sgf.com.ar
- **Documentación completa**: https://docs.sgf.com.ar/etl
- **Issue tracker**: https://github.com/sgf/etl-mappings/issues

---

*Última actualización: Mayo 2024*  
*Versión del documento: 1.0*
