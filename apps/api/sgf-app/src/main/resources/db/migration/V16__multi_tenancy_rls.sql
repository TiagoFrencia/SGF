-- V16: Multi-Tenancy Row-Level Security

-- Create tenants master table first so tenant columns can reference it consistently.
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(36) PRIMARY KEY,
    slug VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    cuit VARCHAR(20) UNIQUE,
    plan VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenants (id, slug, business_name, cuit, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'farmacia-demo', 'Farmacia Demo S.A.', '30-12345678-9', 'PROFESSIONAL')
ON CONFLICT DO NOTHING;

-- Add tenant_id to all current entities backed by BaseEntity or required by tenant-aware flows.
ALTER TABLE IF EXISTS roles ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS products ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS product_presentations ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS batches ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS stock_movements ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS sales ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS sale_items ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS audit_events ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS branch_transfers ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS pos_orders ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS pos_order_items ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS adesfa_validations ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS afip_invoices ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS anmat_remediation_cases ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE IF EXISTS anmat_traceability_events ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);

-- Backfill existing rows and enforce the baseline tenant contract.
UPDATE roles SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE products SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE product_presentations SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE batches SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE stock_movements SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE sales SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE sale_items SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE audit_events SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE branch_transfers SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE pos_orders SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE pos_order_items SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE adesfa_validations SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE afip_invoices SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE anmat_remediation_cases SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE anmat_traceability_events SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

ALTER TABLE IF EXISTS roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS product_presentations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS batches ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS stock_movements ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS sales ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS sale_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS audit_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS branch_transfers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS pos_orders ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS pos_order_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS adesfa_validations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS afip_invoices ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS anmat_remediation_cases ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE IF EXISTS anmat_traceability_events ALTER COLUMN tenant_id SET NOT NULL;

-- Enable Row Level Security on the main business tables already used in the modular monolith.
ALTER TABLE IF EXISTS products ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS pos_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS branch_transfers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_products ON products;
CREATE POLICY tenant_isolation_products ON products
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation_batches ON batches;
CREATE POLICY tenant_isolation_batches ON batches
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation_sales ON sales;
CREATE POLICY tenant_isolation_sales ON sales
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation_sale_items ON sale_items;
CREATE POLICY tenant_isolation_sale_items ON sale_items
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation_pos_orders ON pos_orders;
CREATE POLICY tenant_isolation_pos_orders ON pos_orders
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

DROP POLICY IF EXISTS tenant_isolation_branch_transfers ON branch_transfers;
CREATE POLICY tenant_isolation_branch_transfers ON branch_transfers
    USING (tenant_id = current_setting('app.tenant_id', TRUE)
           OR current_setting('app.tenant_id', TRUE) IS NULL);

CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_tenant ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_presentations_tenant ON product_presentations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_batches_tenant ON batches(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sales_tenant ON sales(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_tenant ON sale_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_tenant ON audit_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_branch_transfers_tenant ON branch_transfers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pos_orders_tenant ON pos_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pos_order_items_tenant ON pos_order_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_adesfa_validations_tenant ON adesfa_validations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_afip_invoices_tenant ON afip_invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_anmat_remediation_cases_tenant ON anmat_remediation_cases(tenant_id);
CREATE INDEX IF NOT EXISTS idx_anmat_traceability_events_tenant ON anmat_traceability_events(tenant_id);
