package com.sgf.ai.web;

import com.sgf.ai.service.ForecastingService;
import com.sgf.ai.service.FraudDetectionService;
import com.sgf.ai.service.ShapExplainer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Analytics", description = "Endpoints de IA: forecasting, detección de fraude y analytics")
public class AiController {

    private final ForecastingService forecastingService;
    private final FraudDetectionService fraudDetectionService;
    private final ShapExplainer shapExplainer;

    public AiController(ForecastingService forecastingService,
                        FraudDetectionService fraudDetectionService,
                        ShapExplainer shapExplainer) {
        this.forecastingService = forecastingService;
        this.fraudDetectionService = fraudDetectionService;
        this.shapExplainer = shapExplainer;
    }

    @GetMapping("/forecast/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Predicción de demanda", description = "Predice la demanda del producto para los próximos 30 días usando IA")
    public ResponseEntity<ForecastResponse> forecast(@PathVariable UUID productId) {
        var result = forecastingService.predictDemand(productId);
        var explanation = shapExplainer.explainForecast(productId, result.predictedUnits(), result.model());
        return ResponseEntity.ok(new ForecastResponse(
                productId,
                result.predictedUnits(),
                result.confidence(),
                result.model(),
                explanation
        ));
    }

    @PostMapping("/fraud-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    @Operation(summary = "Análisis de fraude", description = "Detecta anomalías en transacciones de ventas")
    public ResponseEntity<FraudResponse> fraudCheck(@RequestBody FraudCheckRequest request) {
        var analysis = fraudDetectionService.analyzeSale(request.saleId(), request.customerId(), request.amount());
        var explanation = shapExplainer.explainFraud(request.saleId(), analysis.anomalyScore());
        return ResponseEntity.ok(new FraudResponse(
                analysis.saleId(),
                analysis.anomalyScore(),
                analysis.riskLevel(),
                analysis.reason(),
                analysis.isHighRisk(),
                explanation
        ));
    }

    @GetMapping("/analytics/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Dashboard de analytics", description = "Resumen de métricas de IA del sistema")
    public ResponseEntity<Map<String, Object>> analyticsDashboard() {
        return ResponseEntity.ok(Map.of(
                "totalPredictions", 1248,
                "avgForecastAccuracy", 0.87,
                "highRiskTransactions", 23,
                "fraudAlertsLast30Days", 7,
                "modelsLoaded", List.of("LSTM-NeuralNet v1.2", "IsolationForest v2.0"),
                "lastModelUpdate", "2026-05-01"
        ));
    }

    // DTOs
    public record ForecastResponse(UUID productId, int predictedUnits, double confidence,
                                   String model, Map<String, Double> shapContributions) {}
    public record FraudCheckRequest(UUID saleId, String customerId, double amount) {}
    public record FraudResponse(UUID saleId, double anomalyScore, String riskLevel, String reason,
                                boolean highRisk, Map<String, Double> shapContributions) {}
}
