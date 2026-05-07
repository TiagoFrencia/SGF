-- V17: enable TimescaleDB when available to finalize AI-ready storage baseline.
-- The migration is safe in environments where the extension is not installed.
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS timescaledb;
EXCEPTION
    WHEN undefined_file OR insufficient_privilege THEN
        RAISE NOTICE 'timescaledb extension not available/allowed in this environment; continuing without it';
END $$;
