-- V18: hardening for immutable audit chain
ALTER TABLE audit_events
    ALTER COLUMN previous_hash SET NOT NULL,
    ALTER COLUMN hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_events_hash ON audit_events(hash);
