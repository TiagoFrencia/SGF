-- V18: hardening for immutable audit chain
ALTER TABLE audit_events
    ALTER COLUMN previous_hash SET NOT NULL,
    ALTER COLUMN hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_events_hash ON audit_events(hash);

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE pos_order_items
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
