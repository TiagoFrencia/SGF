-- V10: AI-Ready features for transactions and analytics
-- Adds contextual columns for ML feature engineering (weather, holidays, epidemic indicators)
-- Enables TimescaleDB-compatible partitioning structure for time-series analytics

-- Extend sales table with AI context features (non-null defaults for existing rows)
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS weather_condition VARCHAR(20) DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS is_holiday BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS local_epidemic_indicator VARCHAR(50) DEFAULT 'NORMAL';

-- Extend audit_events with structured metadata for anomaly detection
ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS source_ip VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent TEXT;

-- Create analytics views for AI feature extraction

-- Daily sales aggregation with weather/holiday context (for demand forecasting)
CREATE OR REPLACE VIEW analytics_daily_sales AS
SELECT
    DATE(s.sold_at) AS sale_date,
    p.id AS product_id,
    p.gtin,
    p.commercial_name,
    COUNT(DISTINCT s.id) AS transaction_count,
    SUM(si.quantity) AS units_sold,
    SUM(si.subtotal) AS revenue,
    MAX(s.weather_condition) AS weather,
    BOOL_OR(s.is_holiday) AS is_holiday,
    MAX(s.local_epidemic_indicator) AS epidemic_status
FROM sales s
JOIN sale_items si ON si.sale_id = s.id
JOIN products p ON p.id = si.product_id
WHERE s.status = 'COMPLETED'
GROUP BY DATE(s.sold_at), p.id, p.gtin, p.commercial_name;

-- Hourly sales pattern for peak-hour detection
CREATE OR REPLACE VIEW analytics_hourly_patterns AS
SELECT
    EXTRACT(HOUR FROM s.sold_at) AS hour_of_day,
    EXTRACT(DOW FROM s.sold_at) AS day_of_week,
    cat.therapeutic_category,
    COUNT(*) AS sale_count,
    AVG(s.total_amount) AS avg_basket
FROM sales s
JOIN sale_items si ON si.sale_id = s.id
JOIN products p ON p.id = si.product_id
LEFT JOIN (
    SELECT product_id,
           CASE
               WHEN SUM(quantity) > 100 THEN 'HIGH_VOLUME'
               WHEN SUM(quantity) > 30 THEN 'MEDIUM_VOLUME'
               ELSE 'LOW_VOLUME'
           END AS therapeutic_category
    FROM sale_items
    GROUP BY product_id
) AS cat ON cat.product_id = p.id
WHERE s.status = 'COMPLETED'
GROUP BY EXTRACT(HOUR FROM s.sold_at), EXTRACT(DOW FROM s.sold_at), cat.therapeutic_category;

-- Stock-out risk indicators (for reorder point optimization)
CREATE OR REPLACE VIEW analytics_stock_risk AS
SELECT
    p.id AS product_id,
    p.commercial_name,
    SUM(b.available_quantity) AS current_stock,
    COALESCE(daily_stats.avg_daily_demand, 0) AS avg_daily_demand,
    CASE
        WHEN COALESCE(daily_stats.avg_daily_demand, 0) = 0 THEN 999
        ELSE SUM(b.available_quantity) / daily_stats.avg_daily_demand
    END AS days_of_stock,
    MIN(b.expires_at) AS next_expiry_date,
    SUM(b.unit_cost * b.available_quantity) AS inventory_value
FROM products p
JOIN batches b ON b.product_id = p.id
LEFT JOIN (
    SELECT si.product_id,
           AVG(daily.units) AS avg_daily_demand
    FROM sale_items si
    JOIN sales s ON s.id = si.sale_id
    JOIN LATERAL (
        SELECT DATE(s.sold_at) AS sale_date, SUM(si2.quantity) AS units
        FROM sale_items si2
        JOIN sales s2 ON s2.id = si2.sale_id
        WHERE si2.product_id = si.product_id
          AND s2.sold_at >= NOW() - INTERVAL '90 days'
        GROUP BY DATE(s2.sold_at)
    ) AS daily ON true
    WHERE s.status = 'COMPLETED'
    GROUP BY si.product_id
) AS daily_stats ON daily_stats.product_id = p.id
WHERE b.available_quantity > 0
GROUP BY p.id, p.commercial_name, daily_stats.avg_daily_demand;

-- Create index for time-series queries (TimescaleDB-compatible if extension available)
CREATE INDEX IF NOT EXISTS idx_sales_sold_at ON sales(sold_at);
CREATE INDEX IF NOT EXISTS idx_sales_status_sold ON sales(status, sold_at);
CREATE INDEX IF NOT EXISTS idx_audit_correlation ON audit_events(correlation_id) WHERE correlation_id IS NOT NULL;
