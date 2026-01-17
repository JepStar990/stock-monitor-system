# start_kafka.sh

#!/bin/bash

# Start Zookeeper
echo "Starting Zookeeper..."
zookeeper-server-start.sh config/zookeeper.properties &

# Wait for Zookeeper to start
sleep 5

# Start Kafka
echo "Starting Kafka..."
kafka-server-start.sh config/server.properties &

# Wait for Kafka to start
sleep 10

# Create topics
echo "Creating Kafka topics..."
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic raw-trades \
  --if-not-exists

kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic aggregated-data \
  --if-not-exists

kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic breakout-alerts \
  --if-not-exists

echo "Kafka setup complete!"
