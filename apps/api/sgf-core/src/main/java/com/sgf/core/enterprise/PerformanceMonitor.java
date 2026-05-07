package com.sgf.core.enterprise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitor de rendimiento y throughput para load testing.
 * 
 * Métricas clave:
 * - Transacciones por segundo (TPS)
 * - Latencia promedio, p95, p99
 * - Tasa de error
 * - Uso de recursos
 * 
 * Objetivo: 100 transacciones/segundo por sucursal
 * 
 * @author SGF Enterprise Team
 */
@Component
public class PerformanceMonitor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);

    /**
     * Registro de métricas por operación.
     */
    private final Map<String, OperationMetrics> metricsRegistry = new ConcurrentHashMap<>();

    /**
     * Threshold objetivo: 100 TPS por sucursal.
     */
    private static final long TARGET_TPS = 100;

    /**
     * Registra el inicio de una operación.
     * 
     * @param operationName Nombre de la operación (ej: "POS_CHECKOUT", "INVENTORY_UPDATE")
     * @return Contexto de medición
     */
    public MeasurementContext startMeasurement(String operationName) {
        return new MeasurementContext(operationName, Instant.now());
    }

    /**
     * Registra el completion de una operación exitosa.
     * 
     * @param operationName Nombre de la operación
     * @param durationMs Duración en milisegundos
     */
    public void recordSuccess(String operationName, long durationMs) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.recordSuccess(durationMs);
    }

    /**
     * Registra el fallo de una operación.
     * 
     * @param operationName Nombre de la operación
     * @param errorCode Código de error
     */
    public void recordFailure(String operationName, String errorCode) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.recordFailure(errorCode);
    }

    /**
     * Obtiene las métricas actuales para una operación.
     * 
     * @param operationName Nombre de la operación
     * @return Métricas agregadas
     */
    public MetricsSnapshot getMetrics(String operationName) {
        OperationMetrics metrics = metricsRegistry.get(operationName);
        if (metrics == null) {
            return MetricsSnapshot.empty();
        }
        return metrics.createSnapshot();
    }

    /**
     * Verifica si se cumple el objetivo de throughput.
     * 
     * @param operationName Nombre de la operación
     * @return true si TPS >= 100
     */
    public boolean meetsTargetThroughput(String operationName) {
        MetricsSnapshot snapshot = getMetrics(operationName);
        return snapshot.getTps() >= TARGET_TPS;
    }

    /**
     * Genera reporte de todas las métricas.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== PERFORMANCE REPORT ===\n\n");
        
        for (Map.Entry<String, OperationMetrics> entry : metricsRegistry.entrySet()) {
            MetricsSnapshot snapshot = entry.getValue().createSnapshot();
            report.append(String.format("Operación: %s\n", entry.getKey()));
            report.append(String.format("  TPS: %.2f (target: %d)\n", snapshot.getTps(), TARGET_TPS));
            report.append(String.format("  Latencia avg: %dms\n", snapshot.getAvgLatencyMs()));
            report.append(String.format("  Latencia p95: %dms\n", snapshot.getP95LatencyMs()));
            report.append(String.format("  Latencia p99: %dms\n", snapshot.getP99LatencyMs()));
            report.append(String.format("  Total requests: %d\n", snapshot.getTotalRequests()));
            report.append(String.format("  Success rate: %.2f%%\n", snapshot.getSuccessRate()));
            report.append("\n");
        }
        
        return report.toString();
    }

    private OperationMetrics getOrCreateMetrics(String operationName) {
        return metricsRegistry.computeIfAbsent(operationName, k -> new OperationMetrics());
    }

    /**
     * Contexto temporal para medición de operaciones.
     */
    public static class MeasurementContext {
        private final String operationName;
        private final Instant startTime;

        public MeasurementContext(String operationName, Instant startTime) {
            this.operationName = operationName;
            this.startTime = startTime;
        }

        /**
         * Finaliza la medición y registra éxito.
         */
        public void endSuccess(PerformanceMonitor monitor) {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            monitor.recordSuccess(operationName, durationMs);
        }

        /**
         * Finaliza la medición y registra fallo.
         */
        public void endFailure(PerformanceMonitor monitor, String errorCode) {
            monitor.recordFailure(operationName, errorCode);
        }
    }

    /**
     * Métricas agregadas por operación.
     */
    private static class OperationMetrics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successfulRequests = new AtomicLong(0);
        private final AtomicLong totalLatencyMs = new AtomicLong(0);
        private final AtomicLong maxLatencyMs = new AtomicLong(0);
        private final Map<String, AtomicLong> errorsByCode = new ConcurrentHashMap<>();
        private final long[] latencyBuckets = new long[100]; // Para percentiles
        private final AtomicLong bucketIndex = new AtomicLong(0);

        public synchronized void recordSuccess(long durationMs) {
            totalRequests.incrementAndGet();
            successfulRequests.incrementAndGet();
            totalLatencyMs.addAndGet(durationMs);
            
            if (durationMs > maxLatencyMs.get()) {
                maxLatencyMs.set(durationMs);
            }

            // Guardar para cálculo de percentiles (sampling)
            int idx = (int)(bucketIndex.getAndIncrement() % 100);
            latencyBuckets[idx] = durationMs;
        }

        public void recordFailure(String errorCode) {
            totalRequests.incrementAndGet();
            errorsByCode.computeIfAbsent(errorCode, k -> new AtomicLong(0)).incrementAndGet();
        }

        public MetricsSnapshot createSnapshot() {
            long total = totalRequests.get();
            long success = successfulRequests.get();
            long latencySum = totalLatencyMs.get();
            
            double tps = total / 60.0; // Simplificado: requests por minuto / 60
            long avgLatency = total > 0 ? latencySum / total : 0;
            double successRate = total > 0 ? (success * 100.0 / total) : 0;
            
            // Calcular percentiles aproximados
            long p95 = calculatePercentile(95);
            long p99 = calculatePercentile(99);

            return new MetricsSnapshot(tps, avgLatency, p95, p99, total, successRate);
        }

        private long calculatePercentile(int percentile) {
            // Simplificado: ordenar buckets y tomar valor
            long[] sorted = latencyBuckets.clone();
            java.util.Arrays.sort(sorted);
            int idx = (sorted.length * percentile) / 100;
            return sorted[Math.min(idx, sorted.length - 1)];
        }
    }

    /**
     * Snapshot inmutable de métricas.
     */
    public static class MetricsSnapshot {
        private final double tps;
        private final long avgLatencyMs;
        private final long p95LatencyMs;
        private final long p99LatencyMs;
        private final long totalRequests;
        private final double successRate;

        public MetricsSnapshot(double tps, long avgLatencyMs, long p95LatencyMs, 
                              long p99LatencyMs, long totalRequests, double successRate) {
            this.tps = tps;
            this.avgLatencyMs = avgLatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.p99LatencyMs = p99LatencyMs;
            this.totalRequests = totalRequests;
            this.successRate = successRate;
        }

        public static MetricsSnapshot empty() {
            return new MetricsSnapshot(0, 0, 0, 0, 0, 100.0);
        }

        public double getTps() { return tps; }
        public long getAvgLatencyMs() { return avgLatencyMs; }
        public long getP95LatencyMs() { return p95LatencyMs; }
        public long getP99LatencyMs() { return p99LatencyMs; }
        public long getTotalRequests() { return totalRequests; }
        public double getSuccessRate() { return successRate; }
    }
}
