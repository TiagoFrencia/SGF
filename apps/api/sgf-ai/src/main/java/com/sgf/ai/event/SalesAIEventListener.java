package com.sgf.ai.event;

import com.sgf.ai.service.ForecastingService;
import com.sgf.core.event.SaleCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Connects the POS flow with the AI Module (Phase 3).
 * Listens for completed sales and triggers re-forecasting.
 */
@Component
public class SalesAIEventListener {

    private static final Logger log = LoggerFactory.getLogger(SalesAIEventListener.class);
    private final ForecastingService forecastingService;

    public SalesAIEventListener(ForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @Async
    @EventListener
    public void onSaleCompleted(SaleCompletedEvent event) {
        log.info("AI Module: Noticed sale {}. Triggering incremental demand forecast updates.", event.saleId());
        
        for (var item : event.items()) {
            try {
                log.debug("AI Module: Updating forecast for product {}", item.productId());
                var forecast = forecastingService.predictDemand(item.productId()); 
                log.debug("AI Module: New forecast for {}: {} units (confidence: {})", 
                    item.productId(), forecast.predictedUnits(), forecast.confidence());
            } catch (Exception e) {
                log.warn("AI Module: Failed to process incremental forecast update for product {} in sale {}", 
                    item.productId(), event.saleId());
            }
        }
    }
}
