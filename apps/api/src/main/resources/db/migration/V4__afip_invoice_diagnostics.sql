ALTER TABLE afip_invoices
    ADD COLUMN afip_result_code VARCHAR(10),
    ADD COLUMN observations_json TEXT,
    ADD COLUMN errors_json TEXT,
    ADD COLUMN last_error_code VARCHAR(50),
    ADD COLUMN last_error_message TEXT,
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_attempted_at TIMESTAMPTZ,
    ADD COLUMN token_expires_at TIMESTAMPTZ;
