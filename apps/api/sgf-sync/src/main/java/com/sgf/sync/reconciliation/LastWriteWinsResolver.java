package com.sgf.sync.reconciliation;

/**
 * Conflict resolution strategy: Last-Write-Wins + audit log.
 * <p>
 * In multi-sucursal scenarios where the same record can be modified
 * offline in two locations, this strategy:
 * - Accepts the write with the most recent timestamp
 * - Logs the discarded change to an audit table for manual review
 * <p>
 * Future strategies could include:
 * - CRDT-based (Conflict-free Replicated Data Types)
 * - Three-way merge for inventory counts
 * - Domain-specific reconciliation rules
 */
public class LastWriteWinsResolver {

    public record TimestampedValue(String entityId, String field, String value, String timestamp) {
    }

    /**
     * Resolves a conflict between two versions of the same field.
     * @return the winning value and whether reconciliation is needed
     */
    public ResolutionResult resolve(TimestampedValue local, TimestampedValue remote) {
        if (local == null) {
            return new ResolutionResult(remote.value(), false, null);
        }
        if (remote == null) {
            return new ResolutionResult(local.value(), false, null);
        }
        if (local.value().equals(remote.value())) {
            return new ResolutionResult(local.value(), false, null);
        }

        // Last-write-wins by timestamp
        int comparison = local.timestamp().compareTo(remote.timestamp());
        if (comparison >= 0) {
            return new ResolutionResult(
                    local.value(),
                    true,
                    "CONFLICT: local=" + local.value() + " remote=" + remote.value() + " -> local wins (newer)"
            );
        } else {
            return new ResolutionResult(
                    remote.value(),
                    true,
                    "CONFLICT: local=" + local.value() + " remote=" + remote.value() + " -> remote wins (newer)"
            );
        }
    }

    public record ResolutionResult(String winningValue, boolean hadConflict, String auditNote) {
    }
}