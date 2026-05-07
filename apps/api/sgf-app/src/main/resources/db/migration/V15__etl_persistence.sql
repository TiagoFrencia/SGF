-- V15: ETL Persistence Schema
CREATE TABLE etl_migration_runs (
    id UUID PRIMARY KEY,
    migration_id VARCHAR(50) UNIQUE NOT NULL,
    source_system VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_records BIGINT DEFAULT 0,
    extracted_count BIGINT DEFAULT 0,
    transformed_count BIGINT DEFAULT 0,
    passed_count BIGINT DEFAULT 0,
    failed_count BIGINT DEFAULT 0,
    warning_count BIGINT DEFAULT 0,
    loaded_count BIGINT DEFAULT 0,
    dry_run BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_batch_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    connection_string TEXT
);

CREATE TABLE etl_migration_failures (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES etl_migration_runs(id),
    gtin VARCHAR(20),
    sku VARCHAR(50),
    commercial_name TEXT,
    error_message TEXT NOT NULL,
    raw_data JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_etl_failures_run ON etl_migration_failures(run_id);
CREATE INDEX idx_etl_runs_status ON etl_migration_runs(status);
