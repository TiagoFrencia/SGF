-- ============================================================
-- V11: Branch Transfers, Reorder Points & Expiry Alerts
-- Phase 2 - Core Operational Modules
-- ============================================================

-- 0. Branches: minimal operational branch baseline used by POS and transfers
CREATE TABLE IF NOT EXISTS branches (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                  VARCHAR(50) NOT NULL UNIQUE,
    name                  VARCHAR(150) NOT NULL,
    active                BOOLEAN NOT NULL DEFAULT true,
    address               TEXT,
    phone                 VARCHAR(50),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO branches (id, code, name, active)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'CENTRAL', 'Sucursal Central', true)
ON CONFLICT (code) DO NOTHING;

-- 1. Branch Transfers: stock movement between branches
CREATE TABLE IF NOT EXISTS branch_transfers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_branch_id      UUID NOT NULL REFERENCES branches(id),
    destination_branch_id UUID NOT NULL REFERENCES branches(id),
    product_id            UUID NOT NULL REFERENCES products(id),
    batch_id              UUID NOT NULL REFERENCES batches(id),
    quantity              INTEGER NOT NULL CHECK (quantity > 0),
    received_quantity     INTEGER,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','IN_TRANSIT','RECEIVED','CANCELLED','DISPUTED')),
    notes                 TEXT,
    shipped_at            TIMESTAMPTZ,
    received_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_branch_transfers_source
    ON branch_transfers(source_branch_id, status);
CREATE INDEX IF NOT EXISTS idx_branch_transfers_dest
    ON branch_transfers(destination_branch_id, status);

-- 2. Reorder Points: dynamic stock thresholds
CREATE TABLE IF NOT EXISTS reorder_points (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id            UUID NOT NULL REFERENCES products(id) UNIQUE,
    current_stock         INTEGER NOT NULL DEFAULT 0,
    avg_daily_demand      DECIMAL(10,2),
    reorder_point         INTEGER NOT NULL,
    eoq                   INTEGER NOT NULL,
    safety_stock          INTEGER NOT NULL DEFAULT 3,
    lead_time_days        INTEGER NOT NULL DEFAULT 7,
    analysis_window_days  INTEGER NOT NULL DEFAULT 90,
    total_demand_window   INTEGER,
    demand_stddev         DOUBLE PRECISION,
    needs_reorder         BOOLEAN NOT NULL DEFAULT false,
    calculated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reorder_points_needs
    ON reorder_points(needs_reorder);

-- 3. Expiry Alert Log: records each alert dispatch
CREATE TABLE IF NOT EXISTS expiry_alerts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id              UUID NOT NULL REFERENCES batches(id),
    product_id            UUID NOT NULL REFERENCES products(id),
    product_name          VARCHAR(200) NOT NULL,
    lot_number            VARCHAR(100) NOT NULL,
    expires_at            DATE NOT NULL,
    available_quantity    INTEGER NOT NULL,
    days_until_expiry     INTEGER NOT NULL,
    severity              VARCHAR(10) NOT NULL
                          CHECK (severity IN ('WARNING','ACTION','CRITICAL')),
    dispatched_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_expiry_alerts_severity
    ON expiry_alerts(severity, dispatched_at);

-- Additional index for StockMovement date-range queries (Fase 2)
CREATE INDEX IF NOT EXISTS idx_stock_movement_type_date
    ON stock_movements(movement_type, occurred_at);

-- Additional index for Batch expiry queries
CREATE INDEX IF NOT EXISTS idx_batches_expiry_qty
    ON batches(expires_at, available_quantity);
