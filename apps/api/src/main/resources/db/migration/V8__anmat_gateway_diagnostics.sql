ALTER TABLE anmat_traceability_events
    ADD COLUMN provider_reference VARCHAR(120),
    ADD COLUMN integration_mode VARCHAR(20),
    ADD COLUMN last_http_status INTEGER,
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;
