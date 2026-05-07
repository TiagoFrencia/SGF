-- V16: Multi-Tenancy Row-Level Security
-- Add tenant_id to all core tables
ALTER TABLE products ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE product_catalog ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE inventory_lots ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);

-- Create tenants master table
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(36) PRIMARY KEY,
    slug VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    cuit VARCHAR(20) UNIQUE,
    plan VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Enable Row Level Security
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_lots ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_items ENABLE ROW LEVEL SECURITY;

-- Policies: each tenant sees only its own data
DROP POLICY IF EXISTS tenant_isolation ON products;
CREATE POLICY tenant_isolation ON products
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation ON inventory_lots;
CREATE POLICY tenant_isolation ON inventory_lots
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation ON sales;
CREATE POLICY tenant_isolation ON sales
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation ON sale_items;
CREATE POLICY tenant_isolation ON sale_items
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

-- Index for performance
CREATE INDEX IF NOT EXISTS idx_products_tenant ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sales_tenant ON sales(tenant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_lots_tenant ON inventory_lots(tenant_id);

-- Seed: default tenant for development
INSERT INTO tenants (id, slug, business_name, cuit, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'farmacia-demo', 'Farmacia Demo S.A.', '30-12345678-9', 'PROFESSIONAL')
ON CONFLICT DO NOTHING;
