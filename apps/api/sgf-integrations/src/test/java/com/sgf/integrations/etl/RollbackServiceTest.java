package com.sgf.integrations.etl;

import com.sgf.audit.service.AuditService;
import com.sgf.catalog.service.ProductService;
import com.sgf.integrations.etl.RollbackService.RollbackPlan;
import com.sgf.integrations.etl.RollbackService.RollbackResult;
import com.sgf.integrations.etl.RollbackService.RollbackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RollbackService.
 * 
 * Tests cover:
 * - Rollback plan creation
 * - Batch tracking during migration
 * - Full rollback execution
 * - Preview (dry-run) rollback
 * - Cleanup after successful migration
 * - Error handling
 */
class RollbackServiceTest {

    private AuditService auditService;
    private ProductService productService;
    private RollbackService rollbackService;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        productService = mock(ProductService.class);
        rollbackService = new RollbackService(auditService, productService);
    }

    @Test
    @DisplayName("Should create rollback plan with pre-migration snapshot")
    void testCreatePlan() {
        String migrationId = "mig-test-001";
        
        RollbackService.RollbackPlan plan = rollbackService.createPlan(
            migrationId, "FarmaWin", 500L
        );
        
        assertNotNull(plan);
        assertEquals(migrationId, plan.migrationId);
        assertEquals("FarmaWin", plan.sourceSystem);
        assertEquals(500L, plan.preCount);
        assertEquals(RollbackStatus.READY, plan.status);
        assertNotNull(plan.createdAt);
        assertTrue(plan.loadedIds.isEmpty());
    }

    @Test
    @DisplayName("Should track loaded batch IDs")
    void testTrackBatch() {
        String migrationId = "mig-track-001";
        rollbackService.createPlan(migrationId, "Nixfarma", 300L);
        
        List<String> batchIds = List.of(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        
        rollbackService.trackBatch(migrationId, batchIds);
        
        RollbackService.RollbackPlan plan = rollbackService.getPlan(migrationId);
        assertEquals(3, plan.loadedIds.size());
        assertTrue(plan.loadedIds.containsAll(batchIds));
    }

    @Test
    @DisplayName("Should execute full rollback and delete all tracked records")
    void testRollbackExecution() {
        String migrationId = "mig-rollback-001";
        rollbackService.createPlan(migrationId, "DBF", 200L);
        
        // Track some batches
        List<String> batch1 = List.of(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        List<String> batch2 = List.of(
            UUID.randomUUID().toString()
        );
        
        rollbackService.trackBatch(migrationId, batch1);
        rollbackService.trackBatch(migrationId, batch2);
        
        // Execute rollback
        RollbackResult result = rollbackService.rollback(migrationId);
        
        assertEquals("SUCCESS", result.status());
        assertEquals(3, result.deletedCount());
        assertEquals(200L, result.preCount());
        verify(productService, times(3)).delete(any(UUID.class));
        verify(auditService).record(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle partial rollback when some deletes fail")
    void testPartialRollback() {
        String migrationId = "mig-partial-001";
        rollbackService.createPlan(migrationId, "FarmaWin", 100L);
        
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        
        rollbackService.trackBatch(migrationId, List.of(id1, id2));
        
        // Simulate one failure
        doThrow(new RuntimeException("Product not found"))
            .when(productService).delete(UUID.fromString(id1));
        
        RollbackResult result = rollbackService.rollback(migrationId);
        
        assertEquals("PARTIAL", result.status());
        assertEquals(1, result.deletedCount());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    @DisplayName("Should return NOT_FOUND when rolling back non-existent migration")
    void testRollbackNotFound() {
        RollbackResult result = rollbackService.rollback("nonexistent");
        
        assertEquals("NOT_FOUND", result.status());
        assertEquals(0, result.deletedCount());
        assertTrue(result.message().contains("No rollback plan found"));
    }

    @Test
    @DisplayName("Should prevent double rollback execution")
    void testDoubleRollbackPrevention() {
        String migrationId = "mig-double-001";
        rollbackService.createPlan(migrationId, "Nixfarma", 150L);
        rollbackService.trackBatch(migrationId, List.of(UUID.randomUUID().toString()));
        
        // First rollback
        RollbackResult first = rollbackService.rollback(migrationId);
        assertEquals("SUCCESS", first.status());
        
        // Second rollback should be prevented
        RollbackResult second = rollbackService.rollback(migrationId);
        assertEquals("ALREADY_EXECUTED", second.status());
    }

    @Test
    @DisplayName("Should prevent rollback while in progress")
    void testRollbackInProgressPrevention() {
        String migrationId = "mig-progress-001";
        RollbackPlan plan = rollbackService.createPlan(migrationId, "DBF", 75L);
        plan.status = RollbackStatus.IN_PROGRESS;
        
        RollbackResult result = rollbackService.rollback(migrationId);
        
        assertEquals("IN_PROGRESS", result.status());
        assertTrue(result.message().contains("already in progress"));
    }

    @Test
    @DisplayName("Should preview rollback without executing")
    void testRollbackPreview() {
        String migrationId = "mig-preview-001";
        rollbackService.createPlan(migrationId, "FarmaWin", 400L);
        
        List<String> ids = List.of(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        rollbackService.trackBatch(migrationId, ids);
        
        RollbackService.RollbackPreview preview = rollbackService.preview(migrationId);
        
        assertEquals(migrationId, preview.migrationId());
        assertEquals("FarmaWin", preview.sourceSystem());
        assertEquals(4, preview.recordsToDelete());
        
        // Verify no deletes happened
        verify(productService, never()).delete(any());
    }

    @Test
    @DisplayName("Should return NOT_FOUND for non-existent preview")
    void testPreviewNotFound() {
        RollbackService.RollbackPreview preview = rollbackService.preview("nonexistent");
        
        assertEquals("NOT_FOUND", preview.sourceSystem());
        assertEquals(0, preview.recordsToDelete());
    }

    @Test
    @DisplayName("Should cleanup plan after successful migration")
    void testCleanup() {
        String migrationId = "mig-cleanup-001";
        rollbackService.createPlan(migrationId, "Nixfarma", 250L);
        rollbackService.trackBatch(migrationId, List.of(UUID.randomUUID().toString()));
        
        rollbackService.cleanup(migrationId);
        
        assertNull(rollbackService.getPlan(migrationId));
    }

    @Test
    @DisplayName("Should list all rollback plans")
    void testListPlans() {
        rollbackService.createPlan("mig-1", "FarmaWin", 100L);
        rollbackService.createPlan("mig-2", "Nixfarma", 200L);
        rollbackService.createPlan("mig-3", "DBF", 300L);
        
        List<RollbackPlan> plans = rollbackService.listPlans();
        
        assertEquals(3, plans.size());
        assertTrue(plans.stream().anyMatch(p -> p.sourceSystem.equals("FarmaWin")));
        assertTrue(plans.stream().anyMatch(p -> p.sourceSystem.equals("Nixfarma")));
        assertTrue(plans.stream().anyMatch(p -> p.sourceSystem.equals("DBF")));
    }

    @Test
    @DisplayName("Should handle empty batch tracking gracefully")
    void testEmptyBatchTracking() {
        String migrationId = "mig-empty-001";
        rollbackService.createPlan(migrationId, "FarmaWin", 50L);
        
        rollbackService.trackBatch(migrationId, List.of());
        
        RollbackPlan plan = rollbackService.getPlan(migrationId);
        assertTrue(plan.loadedIds.isEmpty());
    }

    @Test
    @DisplayName("RollbackResult should indicate success correctly")
    void testRollbackResultSuccess() {
        RollbackResult success = new RollbackResult("mig-1", "SUCCESS", 10, 100, "OK");
        RollbackResult partial = new RollbackResult("mig-2", "PARTIAL", 8, 100, "Some failed");
        RollbackResult notFound = new RollbackResult("mig-3", "NOT_FOUND", 0, 0, "Not found");
        
        assertTrue(success.isSuccess());
        assertFalse(partial.isSuccess());
        assertFalse(notFound.isSuccess());
    }

    @Test
    @DisplayName("RollbackPreview should provide description")
    void testRollbackPreviewDescription() {
        RollbackService.RollbackPreview preview = 
            new RollbackService.RollbackPreview("mig-1", "FarmaWin", 50);
        
        String desc = preview.description();
        
        assertNotNull(desc);
        assertTrue(desc.contains("FarmaWin"));
        assertTrue(desc.contains("50"));
        assertTrue(desc.contains("registros"));
    }

    @Test
    @DisplayName("Should track multiple batches correctly")
    void testMultipleBatches() {
        String migrationId = "mig-multi-001";
        rollbackService.createPlan(migrationId, "DBF", 1000L);
        
        // Simulate 10 batches of 100 records each
        for (int i = 0; i < 10; i++) {
            List<String> batch = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                batch.add(UUID.randomUUID().toString());
            }
            rollbackService.trackBatch(migrationId, batch);
        }
        
        RollbackPlan plan = rollbackService.getPlan(migrationId);
        assertEquals(1000, plan.loadedIds.size());
    }
}
