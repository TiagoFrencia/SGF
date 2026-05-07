package com.sgf.integrations.etl;

import com.sgf.integrations.etl.transform.DataTransformer;
import com.sgf.integrations.etl.validate.DataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShadowMode.
 * 
 * Tests cover:
 * - Shadow mode execution for all sources
 * - Quality scoring calculation
 * - Recommendations based on score
 * - Performance benchmarking
 */
class ShadowModeTest {

    private DataTransformer transformer;
    private DataValidator validator;
    private ShadowMode shadowMode;

    @BeforeEach
    void setUp() {
        transformer = mock(DataTransformer.class);
        validator = mock(DataValidator.class);
        shadowMode = new ShadowMode(transformer, validator);
    }

    @Test
    @DisplayName("Should run shadow mode for single source")
    void testRunForSource() {
        // Mock transformer to return clean records
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        // Mock validator to pass all records
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(5);
        when(report.failed()).thenReturn(0);
        when(report.warnings()).thenReturn(0);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult shadowResult = shadowMode.runForSource("FarmaWin");
        
        assertNotNull(shadowResult);
        assertEquals("FarmaWin", shadowResult.source());
        assertTrue(shadowResult.score() >= 0);
        assertNotNull(shadowResult.recommendation());
    }

    @Test
    @DisplayName("Should calculate quality score correctly")
    void testQualityScoreCalculation() {
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(95);
        when(report.failed()).thenReturn(5);
        when(report.warnings()).thenReturn(10);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult result_ = shadowMode.runForSource("Nixfarma");
        
        // Score should be 95% (95 passed out of 100)
        assertEquals(95.0, result_.score(), 0.1);
    }

    @Test
    @DisplayName("Should provide recommendation based on score")
    void testRecommendationByScore() {
        // High score scenario (>95%)
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(98);
        when(report.failed()).thenReturn(2);
        when(report.warnings()).thenReturn(0);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult highScore = shadowMode.runForSource("FarmaWin");
        assertTrue(highScore.recommendation().contains("✓") || 
                   highScore.recommendation().contains("Listo"));
    }

    @Test
    @DisplayName("Should warn for medium scores (80-95%)")
    void testMediumScoreWarning() {
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(85);
        when(report.failed()).thenReturn(15);
        when(report.warnings()).thenReturn(5);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult mediumScore = shadowMode.runForSource("Nixfarma");
        assertTrue(mediumScore.recommendation().contains("⚠") || 
                   mediumScore.recommendation().contains("Revisar"));
    }

    @Test
    @DisplayName("Should recommend against migration for low scores (<60%)")
    void testLowScoreBlocksMigration() {
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(50);
        when(report.failed()).thenReturn(50);
        when(report.warnings()).thenReturn(20);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult lowScore = shadowMode.runForSource("DBF");
        assertTrue(lowScore.recommendation().contains("🛑") || 
                   lowScore.recommendation().contains("NO migrar"));
    }

    @Test
    @DisplayName("Should track elapsed time")
    void testElapsedTimeTracking() {
        DataTransformer.TransformResult result = mock(DataTransformer.TransformResult.class);
        when(result.record()).thenReturn(new LegacyProductRecord());
        when(transformer.transform(any())).thenReturn(List.of(result));
        
        DataValidator.ValidationReport report = mock(DataValidator.ValidationReport.class);
        when(report.passed()).thenReturn(100);
        when(report.failed()).thenReturn(0);
        when(report.warnings()).thenReturn(0);
        when(validator.validate(any())).thenReturn(report);
        
        ShadowMode.ShadowSourceResult timedResult = shadowMode.runForSource("FarmaWin");
        
        assertTrue(timedResult.elapsedSeconds() >= 0);
        assertTrue(timedResult.elapsedSeconds() < 60); // Should complete in under a minute
    }

    @Test
    @DisplayName("Should handle unknown source system")
    void testUnknownSourceSystem() {
        assertThrows(IllegalArgumentException.class, () -> {
            shadowMode.runForSource("UnknownSystem");
        });
    }

    @Test
    @DisplayName("Shadow report should compute overall score")
    void testOverallScoreComputation() {
        ShadowMode.ShadowReport report = new ShadowMode.ShadowReport();
        
        // Add multiple source results
        report.addSource(new ShadowMode.ShadowSourceResult(
            "FarmaWin", 100, 90, 10, 5, 100, 90.0, 1.5, "Good"
        ));
        report.addSource(new ShadowMode.ShadowSourceResult(
            "Nixfarma", 200, 180, 20, 10, 200, 90.0, 2.0, "Good"
        ));
        
        report.computeOverallScore();
        
        assertEquals(300, report.totalRecords);
        assertEquals(270, report.totalPassed);
        assertEquals(30, report.totalFailed);
        assertEquals(90.0, report.overallScore, 0.1);
        assertEquals("READY", report.readiness);
    }

    @Test
    @DisplayName("Shadow report should determine readiness status")
    void testReadinessStatus() {
        // Test READY status (>=90%)
        ShadowMode.ShadowReport readyReport = new ShadowMode.ShadowReport();
        readyReport.totalRecords = 100;
        readyReport.totalPassed = 95;
        readyReport.totalFailed = 5;
        readyReport.overallScore = 95.0;
        readyReport.computeOverallScore();
        assertEquals("READY", readyReport.readiness);
        
        // Test NEEDS_REVIEW status (70-89%)
        ShadowMode.ShadowReport reviewReport = new ShadowMode.ShadowReport();
        reviewReport.totalRecords = 100;
        reviewReport.totalPassed = 80;
        reviewReport.totalFailed = 20;
        reviewReport.overallScore = 80.0;
        reviewReport.computeOverallScore();
        assertEquals("NEEDS_REVIEW", reviewReport.readiness);
        
        // Test NOT_READY status (<70%)
        ShadowMode.ShadowReport notReadyReport = new ShadowMode.ShadowReport();
        notReadyReport.totalRecords = 100;
        notReadyReport.totalPassed = 60;
        notReadyReport.totalFailed = 40;
        notReadyReport.overallScore = 60.0;
        notReadyReport.computeOverallScore();
        assertEquals("NOT_READY", notReadyReport.readiness);
    }

    @Test
    @DisplayName("Shadow report summary should include all sources")
    void testReportSummary() {
        ShadowMode.ShadowReport report = new ShadowMode.ShadowReport();
        report.addSource(new ShadowMode.ShadowSourceResult(
            "FarmaWin", 100, 90, 10, 5, 100, 90.0, 1.5, "Good"
        ));
        report.computeOverallScore();
        
        String summary = report.summary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("SHADOW MODE REPORT"));
        assertTrue(summary.contains("FarmaWin"));
        assertTrue(summary.contains("90.0%"));
    }

    @Test
    @DisplayName("Should handle zero total records gracefully")
    void testZeroRecords() {
        ShadowMode.ShadowReport report = new ShadowMode.ShadowReport();
        report.totalRecords = 0;
        report.totalPassed = 0;
        report.totalFailed = 0;
        report.computeOverallScore();
        
        assertEquals(0, report.overallScore, 0.1);
    }
}
