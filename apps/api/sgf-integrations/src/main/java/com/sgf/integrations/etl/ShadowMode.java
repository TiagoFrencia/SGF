package com.sgf.integrations.etl;

import com.sgf.integrations.etl.extract.DbfExtractor;
import com.sgf.integrations.etl.extract.FarmaWinExtractor;
import com.sgf.integrations.etl.extract.LegacyExtractor;
import com.sgf.integrations.etl.extract.NixfarmaExtractor;
import com.sgf.integrations.etl.transform.DataTransformer;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import com.sgf.integrations.etl.validate.DataValidator;
import com.sgf.integrations.etl.validate.DataValidator.ValidationReport;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Shadow Mode: runs the full ETL pipeline against legacy data but writes NOTHING.
 *
 * Purpose:
 * - Pre-migration readiness assessment
 * - Performance benchmarking (how long will the real migration take?)
 * - Data quality scoring (what % of records will pass validation?)
 * - Error discovery without risk
 *
 * Usage: run shadow mode first → review report → fix data in legacy system → run again → then do real migration
 */
@Service
public class ShadowMode {

    private static final Logger log = LoggerFactory.getLogger(ShadowMode.class);

    private final DataTransformer transformer;
    private final DataValidator validator;

    public ShadowMode(DataTransformer transformer, DataValidator validator) {
        this.transformer = transformer;
        this.validator = validator;
    }

    /**
     * Run a full shadow migration against all configured extractors.
     *
     * @return ShadowReport with quality scores and recommendations
     */
    public ShadowReport runAll() {
        ShadowReport report = new ShadowReport();

        report.addSource(runForSystem(new FarmaWinExtractor(), "FarmaWin"));
        report.addSource(runForSystem(new NixfarmaExtractor(), "Nixfarma"));
        report.addSource(runForSystem(new DbfExtractor(), "DBF_Generic"));

        report.finalize();
        log.info("Shadow mode complete: {} records across {} sources — overall score: {}/100",
                report.totalRecords, report.sourceCount(), report.overallScore);

        return report;
    }

    /**
     * Run shadow mode for a single source system.
     */
    public ShadowSourceResult runForSource(String sourceSystem) {
        LegacyExtractor extractor = switch (sourceSystem.toLowerCase()) {
            case "farmawin" -> new FarmaWinExtractor();
            case "nixfarma" -> new NixfarmaExtractor();
            case "dbf", "dbf_generic" -> new DbfExtractor();
            default -> throw new IllegalArgumentException("Unknown source: " + sourceSystem);
        };
        return runForSystem(extractor, sourceSystem);
    }

    private ShadowSourceResult runForSystem(LegacyExtractor extractor, String name) {
        long startMs = System.currentTimeMillis();

        try {
            extractor.open(name);

            long total = extractor.totalRecords();
            int passed = 0, failed = 0, transformed = 0, warnings = 0;

            while (extractor.hasMore()) {
                LegacyProductRecord[] batch = extractor.extractBatch();
                List<TransformResult> results = transformer.transform(batch);
                transformed += results.size();
                ValidationReport vReport = validator.validate(results);
                passed += vReport.passed();
                failed += vReport.failed();
                warnings += vReport.warnings();
            }

            long elapsedMs = System.currentTimeMillis() - startMs;
            double score = total > 0 ? (passed * 100.0 / total) : 100.0;

            String recommendation;
            if (score >= 95) {
                recommendation = "✓ Listo para migración. Datos de alta calidad.";
            } else if (score >= 80) {
                recommendation = "⚠ Revisar " + failed + " registros fallidos antes de migrar.";
            } else if (score >= 60) {
                recommendation = "⚠⚠ Corregir datos en el sistema legado (" + failed
                        + " fallidos) y re-ejecutar shadow mode.";
            } else {
                recommendation = "🛑 NO migrar. Menos del 60% de registros son válidos. "
                        + "Requiere limpieza exhaustiva del sistema legado.";
            }

            return new ShadowSourceResult(
                    name, total, passed, failed, warnings, transformed,
                    score, elapsedMs / 1000.0,
                    recommendation
            );
        } finally {
            extractor.close();
        }
    }

    /**
     * Comprehensive shadow report across all sources.
     */
    public static class ShadowReport {
        private final java.util.List<ShadowSourceResult> sources = new java.util.ArrayList<>();
        public int totalRecords;
        public int totalPassed;
        public int totalFailed;
        public int totalWarnings;
        public double overallScore;
        public String readiness;

        void addSource(ShadowSourceResult r) {
            sources.add(r);
            totalRecords += r.total;
            totalPassed += r.passed;
            totalFailed += r.failed;
            totalWarnings += r.warnings;
        }

        void finalize() {
            overallScore = totalRecords > 0 ? (totalPassed * 100.0 / totalRecords) : 0;
            readiness = overallScore >= 90 ? "READY"
                    : overallScore >= 70 ? "NEEDS_REVIEW"
                    : "NOT_READY";
        }

        public List<ShadowSourceResult> sources() { return sources; }
        public int sourceCount() { return sources.size(); }

        public String summary() {
            var sb = new StringBuilder();
            sb.append("=== SHADOW MODE REPORT ===\n");
            sb.append(String.format("Readiness: %s (score: %.1f%%)\n", readiness, overallScore));
            sb.append(String.format("Total: %d records across %d sources\n", totalRecords, sources.size()));
            sb.append(String.format("Passed: %d | Failed: %d | Warnings: %d\n\n", totalPassed, totalFailed, totalWarnings));
            for (var s : sources) {
                sb.append(String.format("[%s] %.1f%% — %d passed, %d failed (%.1fs)\n  %s\n",
                        s.source, s.score, s.passed, s.failed, s.elapsedSeconds, s.recommendation));
            }
            return sb.toString();
        }
    }

    public record ShadowSourceResult(
            String source,
            long total,
            int passed,
            int failed,
            int warnings,
            int transformed,
            double score,
            double elapsedSeconds,
            String recommendation
    ) {}
}