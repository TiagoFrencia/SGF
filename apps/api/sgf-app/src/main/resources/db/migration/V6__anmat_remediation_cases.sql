CREATE TABLE anmat_remediation_cases (
    id UUID PRIMARY KEY,
    gtin VARCHAR(14) NOT NULL,
    serial_number VARCHAR(120) NOT NULL,
    lot_number VARCHAR(120),
    issue_code VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    recommendation VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT,
    assigned_to VARCHAR(100),
    last_action_by VARCHAR(100),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_anmat_remediation UNIQUE (gtin, serial_number, issue_code)
);
