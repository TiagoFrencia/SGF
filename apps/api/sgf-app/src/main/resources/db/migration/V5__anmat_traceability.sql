ALTER TABLE products
    ADD COLUMN requires_traceability BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN anmat_category VARCHAR(50);

CREATE TABLE anmat_traceability_events (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    batch_id UUID REFERENCES batches(id),
    sale_id UUID REFERENCES sales(id),
    event_type VARCHAR(30) NOT NULL,
    event_status VARCHAR(30) NOT NULL,
    gtin VARCHAR(14) NOT NULL,
    serial_number VARCHAR(120) NOT NULL,
    lot_number VARCHAR(120) NOT NULL,
    expires_at DATE NOT NULL,
    gln VARCHAR(20),
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(30) NOT NULL,
    request_json TEXT NOT NULL,
    response_json TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_anmat_serial_event UNIQUE (event_type, gtin, serial_number)
);
