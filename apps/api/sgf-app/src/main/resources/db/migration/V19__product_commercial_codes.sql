-- V19: align product commercial enrichment fields with the current domain model.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS alfabet_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS kairos_code VARCHAR(100);
