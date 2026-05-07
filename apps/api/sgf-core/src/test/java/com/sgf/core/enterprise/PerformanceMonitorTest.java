package com.sgf.core.enterprise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para monitor de performance y load testing.
 */
class PerformanceMonitorTest {

    private PerformanceMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new PerformanceMonitor();
    }

    @Test
    @DisplayName("Debe registrar operación exitosa correctamente")
    void shouldRecordSuccessfulOperation() {
        monitor.recordSuccess("POS_CHECKOUT", 50);
        monitor.recordSuccess("POS_CHECKOUT", 30);
        monitor.recordSuccess("POS_CHECKOUT", 70);
        
        PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("POS_CHECKOUT");
        
        assertEquals(3, metrics.getTotalRequests());
        assertEquals(50, metrics.getAvgLatencyMs()); // (50+30+70)/3
        assertEquals(100.0, metrics.getSuccessRate());
    }

    @Test
    @DisplayName("Debe registrar fallo con código de error")
    void shouldRecordFailureWithErrorCode() {
        monitor.recordSuccess("INVENTORY_UPDATE", 20);
        monitor.recordFailure("INVENTORY_UPDATE", "DB_TIMEOUT");
        monitor.recordSuccess("INVENTORY_UPDATE", 25);
        
        PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("INVENTORY_UPDATE");
        
        assertEquals(3, metrics.getTotalRequests());
        assertEquals(66.67, metrics.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Debe retornar métricas vacías si no hay registros")
    void shouldReturnEmptyMetricsIfNoRecords() {
        PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("NON_EXISTENT");
        
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0, metrics.getTps(), 0.01);
        assertEquals(100.0, metrics.getSuccessRate());
    }

    @Test
    @DisplayName("Debe calcular percentiles correctamente")
    void shouldCalculatePercentilesCorrectly() {
        // Registrar 100 operaciones con latencias conocidas
        for (int i = 1; i <= 100; i++) {
            monitor.recordSuccess("LATENCY_TEST", i);
        }
        
        PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("LATENCY_TEST");
        
        assertTrue(metrics.getP95LatencyMs() > metrics.getAvgLatencyMs());
        assertTrue(metrics.getP99LatencyMs() >= metrics.getP95LatencyMs());
    }

    @Test
    @DisplayName("Debe verificar throughput objetivo")
    void shouldCheckTargetThroughput() {
        // Simular muchas operaciones para alcanzar TPS alto
        for (int i = 0; i < 6000; i++) {
            monitor.recordSuccess("HIGH_VOLUME_OP", 5);
        }
        
        boolean meetsTarget = monitor.meetsTargetThroughput("HIGH_VOLUME_OP");
        
        // Con 6000 requests, debería superar 100 TPS
        assertTrue(meetsTarget);
    }

    @Test
    @DisplayName("Debe generar reporte completo de métricas")
    void shouldGenerateCompleteReport() {
        monitor.recordSuccess("OP_1", 10);
        monitor.recordSuccess("OP_1", 20);
        monitor.recordFailure("OP_2", "ERROR_X");
        
        String report = monitor.generateReport();
        
        assertTrue(report.contains("PERFORMANCE REPORT"));
        assertTrue(report.contains("OP_1"));
        assertTrue(report.contains("OP_2"));
        assertTrue(report.contains("TPS"));
        assertTrue(report.contains("Latencia"));
    }

    @Test
    @DisplayName("Debe usar contexto de medición para timing automático")
    void shouldUseMeasurementContextForAutomaticTiming() {
        PerformanceMonitor.MeasurementContext context = monitor.startMeasurement("TIMED_OP");
        
        try {
            Thread.sleep(50); // Simular trabajo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        context.endSuccess(monitor);
        
        PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("TIMED_OP");
        
        assertEquals(1, metrics.getTotalRequests());
        assertTrue(metrics.getAvgLatencyMs() >= 50);
    }

    @Test
    @DisplayName("Debe manejar múltiples operaciones concurrentes")
    void shouldHandleMultipleConcurrentOperations() throws InterruptedException {
        Thread[] threads = new Thread[10];
        
        for (int t = 0; t < 10; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                String opName = "CONCURRENT_OP_" + threadId;
                for (int i = 0; i < 100; i++) {
                    monitor.recordSuccess(opName, 10 + threadId);
                }
            });
            threads[t].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verificar que todas las operaciones fueron registradas
        for (int t = 0; t < 10; t++) {
            PerformanceMonitor.MetricsSnapshot metrics = monitor.getMetrics("CONCURRENT_OP_" + t);
            assertEquals(100, metrics.getTotalRequests());
        }
    }

    @Test
    @DisplayName("Debe mantener métricas separadas por operación")
    void shouldKeepMetricsSeparateByOperation() {
        monitor.recordSuccess("OP_A", 10);
        monitor.recordSuccess("OP_A", 20);
        monitor.recordSuccess("OP_B", 100);
        
        PerformanceMonitor.MetricsSnapshot metricsA = monitor.getMetrics("OP_A");
        PerformanceMonitor.MetricsSnapshot metricsB = monitor.getMetrics("OP_B");
        
        assertEquals(2, metricsA.getTotalRequests());
        assertEquals(1, metricsB.getTotalRequests());
        assertEquals(15, metricsA.getAvgLatencyMs());
        assertEquals(100, metricsB.getAvgLatencyMs());
    }
}
