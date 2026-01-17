#!/bin/bash
# ^ THIS MUST BE LINE 1

echo "🚀 Starting Stock Market Monitoring Pipeline..."

# 1. Start Docker services (Kafka, DB, Grafana)
echo "📦 Starting Docker services..."
sudo docker compose up -d  # Use 'docker compose' (modern) instead of 'docker-compose'

# 2. Check for JARs before starting
if [ ! -f "java-producer/target/massive-producer-1.0.jar" ]; then
    echo "❌ Producer JAR not found. Building now..."
    (cd java-producer && mvn clean package -DskipTests)
fi

if [ ! -f "spark-processor/target/scala-2.12/stock-monitor-spark_2.12-1.0.jar" ]; then
    echo "❌ Spark JAR not found. Building now..."
    (cd spark-processor && sbt package)
fi

# 3. Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 3

# 4. Start Java Producer
echo "🔌 Starting WebSocket Producer..."
# Use -cp or -jar. Since we used the Shade plugin, -jar is correct.
java -jar java-producer/target/massive-producer-1.0.jar &
PRODUCER_PID=$!

# 5. Start Spark Streaming
echo "⚡ Starting Spark Streaming Processor..."
# Note: Ensure the --packages version matches your Spark install
/opt/spark/bin/spark-submit \
  --class com.stockmonitor.spark.SparkStockProcessor \
  --master "local[*]" \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0,org.postgresql:postgresql:42.7.1 \
  spark-processor/target/scala-2.12/stock-monitor-spark_2.12-1.0.jar &
SPARK_PID=$!

echo "✅ Pipeline started successfully!"
echo $PRODUCER_PID > .producer.pid
echo $SPARK_PID > .spark.pid
