ALTER TABLE products
    ADD COLUMN IF NOT EXISTS laboratory VARCHAR(160),
    ADD COLUMN IF NOT EXISTS laboratory_code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS snomed_code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS troquel VARCHAR(40),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_record_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS source_updated_at DATE;

CREATE INDEX IF NOT EXISTS idx_products_troquel ON products (troquel);
CREATE INDEX IF NOT EXISTS idx_products_snomed_code ON products (snomed_code);
CREATE INDEX IF NOT EXISTS idx_products_source_record ON products (source, source_record_key);

CREATE TABLE IF NOT EXISTS product_price_snapshots (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    source VARCHAR(40) NOT NULL,
    source_record_key VARCHAR(120) NOT NULL,
    effective_date DATE NOT NULL,
    retail_price NUMERIC(12, 2) NOT NULL,
    pami_affiliate_price NUMERIC(12, 2),
    pami_discount_code INTEGER,
    pami_discount_label VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tenant_id VARCHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    CONSTRAINT uq_product_price_snapshot UNIQUE (product_id, source, source_record_key, effective_date)
);

CREATE INDEX IF NOT EXISTS idx_product_price_snapshots_product ON product_price_snapshots (product_id);
CREATE INDEX IF NOT EXISTS idx_product_price_snapshots_effective_date ON product_price_snapshots (effective_date);
