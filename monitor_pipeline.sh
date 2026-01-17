# monitor_pipeline.sh

#!/bin/bash

echo "📊 Pipeline Health Monitor"
echo "========================="

# Check Kafka
echo "Kafka Topics:"
kafka-topics.sh --list --bootstrap-server localhost:9092

echo ""
echo "Consumer Lag:"
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups

echo ""
echo "Database Records:"
psql -U postgres -d stockdb -c "SELECT symbol, COUNT(*) as records, MAX(window_start) as latest FROM stock_aggregates GROUP BY symbol;"

echo ""
echo "Recent Alerts:"
psql -U postgres -d stockdb -c "SELECT * FROM breakout_alerts ORDER BY timestamp DESC LIMIT 10;"
