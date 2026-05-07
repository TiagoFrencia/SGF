package com.sgf.app.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom business metrics for Prometheus/Grafana dashboards.
 * These complement Spring Boot Actuator's auto-configured JVM and HTTP metrics.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter salesCounter(MeterRegistry registry) {
        return Counter.builder("sgf.sales.total")
                .description("Total number of completed sales")
                .tag("currency", "ARS")
                .register(registry);
    }

    @Bean
    public Counter afipErrorsCounter(MeterRegistry registry) {
        return Counter.builder("sgf.afip.errors.total")
                .description("Total number of AFIP invoicing errors")
                .register(registry);
    }

    @Bean
    public Counter fraudAlertsCounter(MeterRegistry registry) {
        return Counter.builder("sgf.fraud.alerts.total")
                .description("Total fraud alerts triggered by AI module")
                .register(registry);
    }

    @Bean
    public Counter aiForecastsCounter(MeterRegistry registry) {
        return Counter.builder("sgf.ai.forecasts.total")
                .description("Total AI demand forecasts generated")
                .register(registry);
    }

    @Bean
    public Timer adesValidationTimer(MeterRegistry registry) {
        return Timer.builder("sgf.adesfa.validation.duration")
                .description("Time to validate a health insurance prescription with ADESFA")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
