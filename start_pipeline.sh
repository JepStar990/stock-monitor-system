# start_pipeline.sh

#!/bin/bash

echo "🚀 Starting Stock Market Monitoring Pipeline..."

# Start Docker services
echo "📦 Starting Docker services..."
docker-compose up -d

# Wait for services
echo "⏳ Waiting for services to be ready..."
sleep 30

# Start Java Producer
echo "🔌 Starting WebSocket Producer..."
cd java-producer
mvn clean package
java -jar target/massive-producer-1.0.jar &
PRODUCER_PID=$!

# Start Spark Streaming
echo "⚡ Starting Spark Streaming Processor..."
cd ../spark-processor
sbt clean package
spark-submit \
  --class com.stockmonitor.spark.SparkStockProcessor \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  target/scala-2.12/stock-monitor-spark_2.12-1.0.jar &
SPARK_PID=$!

echo "✅ Pipeline started successfully!"
echo "Producer PID: $PRODUCER_PID"
echo "Spark PID: $SPARK_PID"
echo ""
echo "📊 Access Grafana at: http://localhost:3000"
echo "   Username: admin, Password: admin"
echo ""
echo "To stop the pipeline, run: ./stop_pipeline.sh"

# Save PIDs for cleanup
echo $PRODUCER_PID > .producer.pid
echo $SPARK_PID > .spark.pid
