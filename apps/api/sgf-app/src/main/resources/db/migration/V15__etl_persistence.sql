-- V15: ETL Persistence Schema alignment

ALTER TABLE IF EXISTS etl_migration_runs
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    ADD COLUMN IF NOT EXISTS total_records BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extracted_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS transformed_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passed_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS warning_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loaded_count BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dry_run BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_batch_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS connection_string TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'extracted_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'extracted_count'
    ) THEN
        ALTER TABLE etl_migration_runs RENAME COLUMN extracted_records TO extracted_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'transformed_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'transformed_count'
    ) THEN
        ALTER TABLE etl_migration_runs RENAME COLUMN transformed_records TO transformed_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'passed_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'passed_count'
    ) THEN
        ALTER TABLE etl_migration_runs RENAME COLUMN passed_records TO passed_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'failed_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'failed_count'
    ) THEN
        ALTER TABLE etl_migration_runs RENAME COLUMN failed_records TO failed_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'loaded_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'etl_migration_runs' AND column_name = 'loaded_count'
    ) THEN
        ALTER TABLE etl_migration_runs RENAME COLUMN loaded_records TO loaded_count;
    END IF;
END $$;

ALTER TABLE IF EXISTS etl_migration_runs
    ALTER COLUMN warning_count TYPE BIGINT USING warning_count::bigint;

CREATE TABLE IF NOT EXISTS etl_migration_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES etl_migration_runs(id) ON DELETE CASCADE,
    gtin VARCHAR(20),
    sku VARCHAR(50),
    commercial_name TEXT,
    error_message TEXT NOT NULL,
    raw_data JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_etl_failures_run ON etl_migration_failures(run_id);
CREATE INDEX IF NOT EXISTS idx_etl_runs_status ON etl_migration_runs(status);
