-- ============================================================================
-- V13__etl_migration_tracking.sql
-- Schema para tracking de migraciones de sistemas legados (Fase 4)
-- ============================================================================

-- Migration runs table: tracks each ETL execution
CREATE TABLE IF NOT EXISTS etl_migration_runs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_id        VARCHAR(64) NOT NULL UNIQUE,
    source_system       VARCHAR(30) NOT NULL,
    connection_string   TEXT,
    tenant_id           VARCHAR(36) REFERENCES tenants(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'STARTED',
        -- STARTED, RUNNING, PAUSED, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, ABORTED
    dry_run             BOOLEAN NOT NULL DEFAULT FALSE,
    total_records       BIGINT NOT NULL DEFAULT 0,
    extracted_records   BIGINT NOT NULL DEFAULT 0,
    transformed_records BIGINT NOT NULL DEFAULT 0,
    passed_records      BIGINT NOT NULL DEFAULT 0,
    failed_records      BIGINT NOT NULL DEFAULT 0,
    warning_count       INT NOT NULL DEFAULT 0,
    loaded_records      BIGINT NOT NULL DEFAULT 0,
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
    migration_id        VARCHAR(64) NOT NULL REFERENCES etl_migration_runs(migration_id) ON DELETE CASCADE,
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
    migration_id        VARCHAR(64) NOT NULL REFERENCES etl_migration_runs(migration_id) ON DELETE CASCADE,
    source_row_id       VARCHAR(255),
    source_system       VARCHAR(30) NOT NULL,
    product_name        VARCHAR(300),
    gtin                VARCHAR(14),
    error_type          VARCHAR(50) NOT NULL,
        -- MISSING_GTIN, INVALID_FORMAT, EXPIRED, MISSING_NAME, VALIDATION, LOAD_ERROR, etc.
    error_details       TEXT NOT NULL,
    raw_data            JSONB,
    repair_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        -- PENDING, REPAIRED, RETRIED, IGNORED, MANUAL_REVIEW
    repair_notes        TEXT,
    repaired_by         VARCHAR(100),
    retried_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Rollback tracking: snapshot for each migration
CREATE TABLE IF NOT EXISTS etl_migration_rollback (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_id        VARCHAR(64) NOT NULL UNIQUE REFERENCES etl_migration_runs(migration_id) ON DELETE CASCADE,
    source_system       VARCHAR(30) NOT NULL,
    pre_migration_count BIGINT NOT NULL,
    loaded_count        BIGINT NOT NULL DEFAULT 0,
    loaded_product_ids  JSONB DEFAULT '[]',
    rollback_status     VARCHAR(20),
        -- READY, IN_PROGRESS, EXECUTED, EXPIRED
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
        -- READY, NEEDS_REVIEW, NOT_READY
    recommendation      TEXT,
    elapsed_seconds     DOUBLE PRECISION,
    run_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    tenant_id           VARCHAR(36) REFERENCES tenants(id)
);

-- Product import audit: links imported products to their migration
CREATE TABLE IF NOT EXISTS etl_imported_products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_id        VARCHAR(64) NOT NULL REFERENCES etl_migration_runs(migration_id) ON DELETE CASCADE,
    product_id          UUID REFERENCES products(id),
    legacy_id           VARCHAR(255),
    legacy_system       VARCHAR(30) NOT NULL,
    import_status       VARCHAR(20) NOT NULL DEFAULT 'IMPORTED',
        -- IMPORTED, UPDATED, SKIPPED (duplicate), MERGED
    imported_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_etl_runs_status ON etl_migration_runs(status);
CREATE INDEX IF NOT EXISTS idx_etl_runs_source ON etl_migration_runs(source_system);
CREATE INDEX IF NOT EXISTS idx_etl_runs_tenant ON etl_migration_runs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_etl_runs_created ON etl_migration_runs(created_at);

CREATE INDEX IF NOT EXISTS idx_etl_batches_migration ON etl_migration_batches(migration_id);
CREATE INDEX IF NOT EXISTS idx_etl_failures_migration ON etl_migration_failures(migration_id);
CREATE INDEX IF NOT EXISTS idx_etl_failures_repair ON etl_migration_failures(repair_status);
CREATE INDEX IF NOT EXISTS idx_etl_failures_gtin ON etl_migration_failures(gtin);

CREATE INDEX IF NOT EXISTS idx_etl_rollback_migration ON etl_migration_rollback(migration_id);
CREATE INDEX IF NOT EXISTS idx_etl_shadow_source ON etl_shadow_reports(source_system);
CREATE INDEX IF NOT EXISTS idx_etl_imported_product ON etl_imported_products(product_id);
CREATE INDEX IF NOT EXISTS idx_etl_imported_migration ON etl_imported_products(migration_id);
CREATE INDEX IF NOT EXISTS idx_etl_imported_legacy ON etl_imported_products(legacy_system, legacy_id);

-- Trigger: auto-update updated_at on migration runs
DO $$ BEGIN
    CREATE TRIGGER trg_etl_runs_updated
        BEFORE UPDATE ON etl_migration_runs
        FOR EACH ROW EXECUTE FUNCTION update_updated_at();
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

COMMENT ON TABLE etl_migration_runs IS 'Fase 4: Registro de ejecuciones de migración ETL';
COMMENT ON TABLE etl_migration_batches IS 'Detalle de lotes procesados por migración';
COMMENT ON TABLE etl_migration_failures IS 'Registros que fallaron validación para revisión manual';
COMMENT ON TABLE etl_migration_rollback IS 'Planes de rollback por migración';
COMMENT ON TABLE etl_shadow_reports IS 'Reportes pre-migración en modo shadow (sin escritura)';
COMMENT ON TABLE etl_imported_products IS 'Trazabilidad de productos importados a su migración origen';