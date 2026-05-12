-- ============================================================================
-- V13__etl_migration_tracking.sql
-- Schema para tracking de migraciones de sistemas legados (Fase 4)
-- ============================================================================

-- Tenants must exist before ETL tracking can reference them.
CREATE TABLE IF NOT EXISTS tenants (
    id                  VARCHAR(36) PRIMARY KEY,
    slug                VARCHAR(50) UNIQUE NOT NULL,
    business_name       VARCHAR(200) NOT NULL,
    cuit                VARCHAR(20) UNIQUE,
    plan                VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO tenants (id, slug, business_name, cuit, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'farmacia-demo', 'Farmacia Demo S.A.', '30-12345678-9', 'PROFESSIONAL')
ON CONFLICT DO NOTHING;

-- Migration runs table: aligns with current JPA entities and leaves room for future ETL metadata.
CREATE TABLE IF NOT EXISTS etl_migration_runs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_id        VARCHAR(64) NOT NULL UNIQUE,
    source_system       VARCHAR(30) NOT NULL,
    connection_string   TEXT,
    tenant_id           VARCHAR(36) REFERENCES tenants(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    dry_run             BOOLEAN NOT NULL DEFAULT FALSE,
    total_records       BIGINT NOT NULL DEFAULT 0,
    extracted_count     BIGINT NOT NULL DEFAULT 0,
    transformed_count   BIGINT NOT NULL DEFAULT 0,
    passed_count        BIGINT NOT NULL DEFAULT 0,
    failed_count        BIGINT NOT NULL DEFAULT 0,
    warning_count       BIGINT NOT NULL DEFAULT 0,
    loaded_count        BIGINT NOT NULL DEFAULT 0,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_batch_at       TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    error_message       TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Migration batch details: per-batch extraction stats
CREATE TABLE IF NOT EXISTS etl_migration_batches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES etl_migration_runs(id) ON DELETE CASCADE,
    batch_number        INT NOT NULL,
    extracted_count     INT NOT NULL DEFAULT 0,
    transformed_count   INT NOT NULL DEFAULT 0,
    passed_count        INT NOT NULL DEFAULT 0,
    failed_count        INT NOT NULL DEFAULT 0,
    loaded_count        INT NOT NULL DEFAULT 0,
    duration_ms         BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Failed records: stored for review, repair, and re-processing
CREATE TABLE IF NOT EXISTS etl_migration_failures (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES etl_migration_runs(id) ON DELETE CASCADE,
    source_row_id       VARCHAR(255),
    source_system       VARCHAR(30) NOT NULL,
    product_name        VARCHAR(300),
    gtin                VARCHAR(14),
    sku                 VARCHAR(60),
    commercial_name     TEXT,
    error_type          VARCHAR(50) NOT NULL,
    error_message       TEXT NOT NULL,
    error_details       TEXT,
    raw_data            JSONB,
    repair_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    repair_notes        TEXT,
    repaired_by         VARCHAR(100),
    retried_at          TIMESTAMPTZ,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Rollback tracking: snapshot for each migration
CREATE TABLE IF NOT EXISTS etl_migration_rollback (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL UNIQUE REFERENCES etl_migration_runs(id) ON DELETE CASCADE,
    source_system       VARCHAR(30) NOT NULL,
    pre_migration_count BIGINT NOT NULL,
    loaded_count        BIGINT NOT NULL DEFAULT 0,
    loaded_product_ids  JSONB DEFAULT '[]',
    rollback_status     VARCHAR(20),
    rollback_requested_at TIMESTAMPTZ,
    rollback_executed_at  TIMESTAMPTZ,
    rollback_deleted     INT,
    rollback_verified    BOOLEAN,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Shadow mode reports: pre-migration readiness assessments
CREATE TABLE IF NOT EXISTS etl_shadow_reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system       VARCHAR(30) NOT NULL,
    total_records       BIGINT NOT NULL,
    passed_records      BIGINT NOT NULL,
    failed_records      BIGINT NOT NULL,
    warning_count       INT NOT NULL DEFAULT 0,
    quality_score       NUMERIC(5,2) NOT NULL,
    readiness           VARCHAR(20) NOT NULL,
    recommendation      TEXT,
    elapsed_seconds     DOUBLE PRECISION,
    run_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    tenant_id           VARCHAR(36) REFERENCES tenants(id)
);

-- Product import audit: links imported products to their migration
CREATE TABLE IF NOT EXISTS etl_imported_products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES etl_migration_runs(id) ON DELETE CASCADE,
    product_id          UUID REFERENCES products(id),
    legacy_id           VARCHAR(255),
    legacy_system       VARCHAR(30) NOT NULL,
    import_status       VARCHAR(20) NOT NULL DEFAULT 'IMPORTED',
    imported_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_etl_runs_status ON etl_migration_runs(status);
CREATE INDEX IF NOT EXISTS idx_etl_runs_source ON etl_migration_runs(source_system);
CREATE INDEX IF NOT EXISTS idx_etl_runs_tenant ON etl_migration_runs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_etl_runs_created ON etl_migration_runs(created_at);

CREATE INDEX IF NOT EXISTS idx_etl_batches_run ON etl_migration_batches(run_id);
CREATE INDEX IF NOT EXISTS idx_etl_failures_run ON etl_migration_failures(run_id);
CREATE INDEX IF NOT EXISTS idx_etl_failures_repair ON etl_migration_failures(repair_status);
CREATE INDEX IF NOT EXISTS idx_etl_failures_gtin ON etl_migration_failures(gtin);

CREATE INDEX IF NOT EXISTS idx_etl_rollback_run ON etl_migration_rollback(run_id);
CREATE INDEX IF NOT EXISTS idx_etl_shadow_source ON etl_shadow_reports(source_system);
CREATE INDEX IF NOT EXISTS idx_etl_imported_product ON etl_imported_products(product_id);
CREATE INDEX IF NOT EXISTS idx_etl_imported_run ON etl_imported_products(run_id);
CREATE INDEX IF NOT EXISTS idx_etl_imported_legacy ON etl_imported_products(legacy_system, legacy_id);

COMMENT ON TABLE etl_migration_runs IS 'Fase 4: Registro de ejecuciones de migracion ETL';
COMMENT ON TABLE etl_migration_batches IS 'Detalle de lotes procesados por migracion';
COMMENT ON TABLE etl_migration_failures IS 'Registros que fallaron validacion para revision manual';
COMMENT ON TABLE etl_migration_rollback IS 'Planes de rollback por migracion';
COMMENT ON TABLE etl_shadow_reports IS 'Reportes pre-migracion en modo shadow (sin escritura)';
COMMENT ON TABLE etl_imported_products IS 'Trazabilidad de productos importados a su migracion origen';
