ALTER TABLE audit_events ADD COLUMN previous_hash TEXT;
ALTER TABLE audit_events ADD COLUMN hash TEXT;
CREATE INDEX idx_audit_events_hash ON audit_events(hash);
