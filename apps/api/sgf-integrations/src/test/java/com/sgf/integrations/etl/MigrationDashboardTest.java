package com.sgf.integrations.etl;

import com.sgf.catalog.service.ProductService;
import com.sgf.core.event.MigrationFinishedEvent;
import com.sgf.core.event.MigrationStartedEvent;
import com.sgf.integrations.etl.domain.EtlMigrationRun;
import com.sgf.integrations.etl.domain.EtlMigrationRunRepository;
import com.sgf.integrations.etl.extract.LegacyExtractor;
import com.sgf.integrations.etl.transform.DataTransformer;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import com.sgf.integrations.etl.validate.DataValidator;
import com.sgf.integrations.etl.validate.DataValidator.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MigrationDashboard.
 * 
 * Tests cover:
 * - Migration start/initialization
 * - Batch execution with progress tracking
 * - Pause/Resume functionality
 * - Abort migration
 * - Dry-run mode
 * - Full migration execution
 * - Event publishing
 */
class MigrationDashboardTest {

    private DataTransformer transformer;
    private DataValidator validator;
    private ProductService productService;
    private ApplicationEventPublisher eventPublisher;
    private RollbackService rollbackService;
    private EtlMigrationRunRepository runRepository;
    private MigrationDashboard dashboard;

    @BeforeEach
    void setUp() {
        transformer = mock(DataTransformer.class);
        validator = mock(DataValidator.class);
        productService = mock(ProductService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        rollbackService = mock(RollbackService.class);
        runRepository = mock(EtlMigrationRunRepository.class);
        
        dashboard = new MigrationDashboard(
            transformer, validator, productService, 
            eventPublisher, rollbackService, runRepository
        );
    }

    @Test
    @DisplayName("Should start migration and return migration ID")
    void testStartMigration() {
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.totalRecords()).thenReturn(100L);
        
        EtlMigrationRun run = new EtlMigrationRun();
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        when(productService.count()).thenReturn(500L);
        
        String migrationId = dashboard.startMigration("FarmaWin", "jdbc:test", false);
        
        assertNotNull(migrationId);
        assertTrue(migrationId.startsWith("FarmaWin-"));
        verify(eventPublisher).publishEvent(any(MigrationStartedEvent.class));
        verify(rollbackService).createPlan(anyString(), eq("FarmaWin"), eq(500L));
    }

    @Test
    @DisplayName("Should start dry-run migration")
    void testStartDryRunMigration() {
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.totalRecords()).thenReturn(100L);
        
        EtlMigrationRun run = new EtlMigrationRun();
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        when(productService.count()).thenReturn(500L);
        
        String migrationId = dashboard.startMigration("Nixfarma", "jdbc:test", true);
        
        assertNotNull(migrationId);
        ArgumentCaptor<EtlMigrationRun> captor = ArgumentCaptor.forClass(EtlMigrationRun.class);
        verify(runRepository).save(captor.capture());
        assertTrue(captor.getValue().isDryRun());
    }

    @Test
    @DisplayName("Should execute batch and return progress")
    void testExecuteBatch() {
        String migrationId = "FarmaWin-test123";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        run.setTotalRecords(100);
        run.setExtractedCount(0);
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.hasMore()).thenReturn(true);
        when(extractor.extractBatch()).thenReturn(new LegacyProductRecord[10]);
        
        // Mock transformer
        TransformResult result = mock(TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        // Mock validator
        ValidationReport report = mock(ValidationReport.class);
        when(report.passed()).thenReturn(10);
        when(report.failed()).thenReturn(0);
        when(report.warnings()).thenReturn(0);
        when(report.passedRecords()).thenReturn(List.of(result));
        when(validator.validate(any())).thenReturn(report);
        
        MigrationDashboard.BatchProgress progress = dashboard.executeBatch(migrationId, 10);
        
        assertNotNull(progress);
        assertEquals(migrationId, progress.migrationId());
        assertEquals(10, progress.extracted());
        verify(runRepository).save(any(EtlMigrationRun.class));
    }

    @Test
    @DisplayName("Should handle pause and resume")
    void testPauseAndResume() {
        String migrationId = "test-pause";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        
        // Pause
        dashboard.pause(migrationId);
        
        ArgumentCaptor<EtlMigrationRun> captor = ArgumentCaptor.forClass(EtlMigrationRun.class);
        verify(runRepository, times(2)).save(captor.capture());
        assertEquals(MigrationDashboard.MigrationStatus.PAUSED.name(), captor.getValue().getStatus());
        
        // Resume
        dashboard.resume(migrationId);
        verify(runRepository, times(3)).save(any(EtlMigrationRun.class));
    }

    @Test
    @DisplayName("Should abort migration and clean up extractor")
    void testAbort() {
        String migrationId = "test-abort";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        
        dashboard.abort(migrationId);
        
        ArgumentCaptor<EtlMigrationRun> captor = ArgumentCaptor.forClass(EtlMigrationRun.class);
        verify(runRepository).save(captor.capture());
        assertEquals(MigrationDashboard.MigrationStatus.ABORTED.name(), captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should get dashboard snapshot")
    void testGetDashboard() {
        String migrationId = "test-dashboard";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setSourceSystem("FarmaWin");
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        run.setTotalRecords(100);
        run.setExtractedCount(50);
        run.setTransformedCount(50);
        run.setPassedCount(48);
        run.setFailedCount(2);
        run.setWarningCount(5);
        run.setLoadedCount(48);
        run.setDryRun(false);
        run.setStartedAt(OffsetDateTime.now());
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        
        MigrationDashboard.DashboardSnapshot snapshot = dashboard.getDashboard(migrationId);
        
        assertNotNull(snapshot);
        assertEquals("FarmaWin", snapshot.sourceSystem());
        assertEquals(50, snapshot.percent());
        assertEquals(48, snapshot.passed());
        assertEquals(2, snapshot.failed());
    }

    @Test
    @DisplayName("Should list all migrations")
    void testListMigrations() {
        EtlMigrationRun run1 = new EtlMigrationRun();
        run1.setMigrationId("mig-1");
        run1.setSourceSystem("FarmaWin");
        run1.setStatus(MigrationDashboard.MigrationStatus.COMPLETED.name());
        
        EtlMigrationRun run2 = new EtlMigrationRun();
        run2.setMigrationId("mig-2");
        run2.setSourceSystem("Nixfarma");
        run2.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        
        when(runRepository.findAll()).thenReturn(List.of(run1, run2));
        
        List<MigrationDashboard.DashboardSnapshot> snapshots = dashboard.listMigrations();
        
        assertEquals(2, snapshots.size());
    }

    @Test
    @DisplayName("Should publish MigrationFinishedEvent on completion")
    void testMigrationFinishEvent() {
        String migrationId = "test-event";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        run.setPassedCount(100);
        run.setFailedCount(0);
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.hasMore()).thenReturn(false);
        
        dashboard.executeBatch(migrationId, 10);
        
        verify(eventPublisher).publishEvent(any(MigrationFinishedEvent.class));
    }

    @Test
    @DisplayName("Should mark migration as COMPLETED_WITH_ERRORS when failures exist")
    void testCompletedWithErrors() {
        String migrationId = "test-errors";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        run.setPassedCount(90);
        run.setFailedCount(10);
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        when(runRepository.save(any(EtlMigrationRun.class))).thenReturn(run);
        
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.hasMore()).thenReturn(false);
        
        dashboard.executeBatch(migrationId, 10);
        
        ArgumentCaptor<EtlMigrationRun> captor = ArgumentCaptor.forClass(EtlMigrationRun.class);
        verify(runRepository).save(captor.capture());
        assertEquals(MigrationDashboard.MigrationStatus.COMPLETED_WITH_ERRORS.name(), 
                    captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should throw exception when migration not found")
    void testMigrationNotFound() {
        when(runRepository.findByMigrationId("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            dashboard.executeBatch("nonexistent", 10);
        });
    }

    @Test
    @DisplayName("Should calculate progress percentage correctly")
    void testProgressCalculation() {
        String migrationId = "test-progress";
        
        EtlMigrationRun run = new EtlMigrationRun();
        run.setMigrationId(migrationId);
        run.setStatus(MigrationDashboard.MigrationStatus.RUNNING.name());
        run.setTotalRecords(200);
        run.setExtractedCount(100);
        
        when(runRepository.findByMigrationId(migrationId)).thenReturn(Optional.of(run));
        
        LegacyExtractor extractor = mock(LegacyExtractor.class);
        when(extractor.hasMore()).thenReturn(true);
        when(extractor.extractBatch()).thenReturn(new LegacyProductRecord[0]);
        
        MigrationDashboard.BatchProgress progress = dashboard.executeBatch(migrationId, 10);
        
        assertEquals(50, progress.percent());
    }
}
