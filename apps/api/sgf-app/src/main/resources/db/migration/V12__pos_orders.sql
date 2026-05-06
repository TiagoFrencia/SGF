-- ============================================================
-- V12: POS Orders & Order Items
-- Phase 2 — Core Operational Modules (Punto de Venta)
-- ============================================================

-- 1. POS Orders: active orders at the point-of-sale terminal
CREATE TABLE IF NOT EXISTS pos_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID        NOT NULL REFERENCES branches(id),
    order_number        INTEGER     NOT NULL,
    customer_name       VARCHAR(200),
    customer_document   VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','READY','COMPLETED','VOIDED')),
    notes               TEXT,
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pos_orders_branch_status
    ON pos_orders(branch_id, status);
CREATE INDEX IF NOT EXISTS idx_pos_orders_branch_ordernum
    ON pos_orders(branch_id, order_number DESC);
CREATE INDEX IF NOT EXISTS idx_pos_orders_number
    ON pos_orders(order_number);

-- 2. POS Order Items: individual line items in a POS order
CREATE TABLE IF NOT EXISTS pos_order_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID        NOT NULL REFERENCES pos_orders(id) ON DELETE CASCADE,
    product_id          UUID        NOT NULL REFERENCES products(id),
    quantity            INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price          DECIMAL(10,2) NOT NULL,
    subtotal            DECIMAL(12,2) NOT NULL,
    batch_id            UUID        REFERENCES batches(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pos_order_items_order
    ON pos_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_pos_order_items_product
    ON pos_order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_pos_order_items_batch
    ON pos_order_items(batch_id) WHERE batch_id IS NOT NULL;

-- 3. Constraint: unique order number per branch per day for DRAFT orders
-- (prevents duplicate order numbers in concurrent sessions)
CREATE UNIQUE INDEX IF NOT EXISTS uq_pos_orders_branch_number_draft
    ON pos_orders(branch_id, order_number)
    WHERE status = 'DRAFT';