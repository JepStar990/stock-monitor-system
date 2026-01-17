-- init_db.sql

CREATE DATABASE stockdb;
\c stockdb

-- Enable TimescaleDB extension (if using TimescaleDB)
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Stock aggregates table
CREATE TABLE stock_aggregates (
    id SERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    avg_price DOUBLE PRECISION NOT NULL,
    max_price DOUBLE PRECISION NOT NULL,
    min_price DOUBLE PRECISION NOT NULL,
    total_volume BIGINT NOT NULL,
    trade_count BIGINT NOT NULL,
    ma_5min DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Convert to hypertable (TimescaleDB feature)
SELECT create_hypertable('stock_aggregates', 'window_start', if_not_exists => TRUE);

-- Create indexes for better query performance
CREATE INDEX idx_stock_symbol ON stock_aggregates(symbol, window_start DESC);
CREATE INDEX idx_stock_time ON stock_aggregates(window_start DESC);

-- Breakout alerts table
CREATE TABLE breakout_alerts (
    id SERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    current_price DOUBLE PRECISION NOT NULL,
    window_avg DOUBLE PRECISION NOT NULL,
    signal VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_symbol ON breakout_alerts(symbol, timestamp DESC);
CREATE INDEX idx_alerts_time ON breakout_alerts(timestamp DESC);

-- View for latest prices and moving averages
CREATE VIEW latest_stock_data AS
SELECT DISTINCT ON (symbol)
    symbol,
    window_start as timestamp,
    avg_price as current_price,
    ma_5min,
    total_volume,
    CASE 
        WHEN avg_price > ma_5min THEN 'BULLISH'
        WHEN avg_price < ma_5min THEN 'BEARISH'
        ELSE 'NEUTRAL'
    END as trend
FROM stock_aggregates
ORDER BY symbol, window_start DESC;

-- Function to get golden cross events
CREATE OR REPLACE FUNCTION get_golden_crosses(
    p_symbol VARCHAR DEFAULT NULL,
    p_hours INT DEFAULT 24
)
RETURNS TABLE (
    symbol VARCHAR,
    cross_time TIMESTAMP,
    price_at_cross DOUBLE PRECISION,
    ma_at_cross DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    WITH price_ma_compare AS (
        SELECT 
            sa.symbol,
            sa.window_start,
            sa.avg_price,
            sa.ma_5min,
            LAG(sa.avg_price) OVER (PARTITION BY sa.symbol ORDER BY sa.window_start) as prev_price,
            LAG(sa.ma_5min) OVER (PARTITION BY sa.symbol ORDER BY sa.window_start) as prev_ma
        FROM stock_aggregates sa
        WHERE (p_symbol IS NULL OR sa.symbol = p_symbol)
          AND sa.window_start > NOW() - (p_hours || ' hours')::INTERVAL
    )
    SELECT 
        pmc.symbol,
        pmc.window_start,
        pmc.avg_price,
        pmc.ma_5min
    FROM price_ma_compare pmc
    WHERE pmc.prev_price <= pmc.prev_ma 
      AND pmc.avg_price > pmc.ma_5min;
END;
$$ LANGUAGE plpgsql;
