# Real-Time Stock Market Monitoring System
## Complete Documentation

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [System Requirements](#system-requirements)
4. [Installation Guide](#installation-guide)
5. [Component Documentation](#component-documentation)
6. [Configuration Guide](#configuration-guide)
7. [Deployment](#deployment)
8. [Monitoring & Troubleshooting](#monitoring--troubleshooting)
9. [Performance Tuning](#performance-tuning)
10. [API Reference](#api-reference)
11. [FAQ](#faq)

---

## Overview

### What is This System?

The Real-Time Stock Market Monitoring System is an end-to-end distributed streaming application designed to process live stock market data, calculate moving averages, detect price breakouts, and visualize trends in real-time. It implements a modern data pipeline using industry-standard technologies.

### Key Features

- **Real-Time Data Ingestion**: WebSocket connection to Massive.com (formerly Polygon.io) for live market data
- **Distributed Processing**: Apache Kafka for reliable message queuing
- **Stream Processing**: Apache Spark Structured Streaming with stateful aggregations
- **Time-Series Storage**: TimescaleDB for efficient time-series data management
- **Interactive Dashboards**: Grafana for professional data visualization
- **Automated Alerts**: Golden Cross and Death Cross detection with notifications
- **Scalable Architecture**: Designed to handle high-volume market data

### Use Cases

- **Day Trading**: Real-time price monitoring with technical indicators
- **Algorithmic Trading**: Foundation for automated trading strategies
- **Market Analysis**: Historical trend analysis and pattern recognition
- **Risk Management**: Early warning system for significant price movements
- **Educational**: Learning distributed systems and stream processing

---

## Architecture

### High-Level Architecture

```
┌─────────────────┐
│  Massive.com    │
│  WebSocket API  │
└────────┬────────┘
         │ Stock Data (WebSocket)
         ▼
┌─────────────────┐
│ Java Producer   │ ◄── Connects to WebSocket
│ (WebSocket      │ ◄── Enriches data
│  Client)        │ ◄── Handles reconnection
└────────┬────────┘
         │ JSON Messages
         ▼
┌─────────────────┐
│  Apache Kafka   │ ◄── Topic: raw-trades
│  (Message       │ ◄── Partitioned by symbol
│   Broker)       │ ◄── Replication factor: 1
└────────┬────────┘
         │ Stream of Records
         ▼
┌─────────────────┐
│ Spark Streaming │ ◄── Structured Streaming
│ (Scala)         │ ◄── 5-min sliding windows
│                 │ ◄── Watermarking (1 min)
│  • Parser       │ ◄── Moving Average calc
│  • Aggregator   │ ◄── Breakout detection
│  • Calculator   │
└────────┬────────┘
         │ Aggregated Data
         ▼
┌─────────────────┐
│  TimescaleDB    │ ◄── Hypertables
│  (PostgreSQL)   │ ◄── Continuous aggregates
│                 │ ◄── Retention policies
└────────┬────────┘
         │ SQL Queries
         ▼
┌─────────────────┐
│    Grafana      │ ◄── Real-time dashboards
│  (Dashboards)   │ ◄── Alert rules
└─────────────────┘ ◄── Email/Slack notifications
```

### Data Flow

1. **Ingestion Layer**
   - WebSocket client connects to Massive.com
   - Receives JSON messages for AAPL, TSLA, BTC
   - Message types: Aggregates (A), Trades (T)

2. **Message Queue Layer**
   - Kafka receives enriched messages
   - Partitioned by stock symbol (3 partitions)
   - Retention: 7 days

3. **Processing Layer**
   - Spark reads from Kafka topic
   - Groups data into 5-minute windows
   - Calculates: avg, max, min, volume, MA
   - Applies 1-minute watermark for late data

4. **Storage Layer**
   - TimescaleDB stores aggregated data
   - Hypertable optimized for time-series
   - Indexes on symbol and timestamp

5. **Visualization Layer**
   - Grafana queries TimescaleDB
   - Real-time charts update every 10s
   - Alerts triggered on breakouts

### Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Data Source | Massive.com API | v2 | Real-time market data |
| Producer | Java | 17+ | WebSocket client |
| Message Queue | Apache Kafka | 3.6.0 | Event streaming |
| Stream Processor | Apache Spark | 3.5.0 | Real-time processing |
| Language | Scala | 2.12.17 | Spark development |
| Database | TimescaleDB | 2.13+ | Time-series storage |
| Base DB | PostgreSQL | 15 | Relational database |
| Visualization | Grafana | 10.0+ | Dashboards & alerts |
| Containerization | Docker | 24.0+ | Service orchestration |

---

## System Requirements

### Hardware Requirements

**Minimum:**
- CPU: 4 cores (2.0 GHz)
- RAM: 8 GB
- Storage: 50 GB SSD
- Network: Broadband (10 Mbps+)

**Recommended:**
- CPU: 8 cores (3.0 GHz)
- RAM: 16 GB
- Storage: 100 GB NVMe SSD
- Network: Fiber (100 Mbps+)

**Production:**
- CPU: 16 cores (3.5 GHz)
- RAM: 32 GB
- Storage: 500 GB NVMe SSD (RAID 10)
- Network: Dedicated 1 Gbps

### Software Requirements

**Operating System:**
- macOS 12+ (Monterey or later)
- Ubuntu 20.04 LTS or later
- RHEL 8+ / CentOS 8+
- Windows 10/11 with WSL2

**Required Software:**
- Java Development Kit (JDK) 17+
- Apache Maven 3.8+
- Scala 2.12.17
- sbt (Scala Build Tool) 1.9+
- Apache Kafka 3.6+
- Apache Spark 3.5+
- PostgreSQL 15+ with TimescaleDB
- Docker 24.0+ & Docker Compose 2.0+
- Git 2.30+

**Optional:**
- IntelliJ IDEA (Java/Scala development)
- DBeaver or pgAdmin (database management)
- Postman (API testing)

### Network Requirements

- **Outbound:** Access to Massive.com WebSocket (`wss://socket.massive.com`)
- **Ports:**
  - 2181: Zookeeper
  - 9092: Kafka
  - 5432: PostgreSQL/TimescaleDB
  - 3000: Grafana
  - 4040: Spark UI

---

## Installation Guide

### Step 1: Install Prerequisites

#### macOS (using Homebrew)

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# Install Maven
brew install maven

# Install Scala and sbt
brew install scala sbt

# Install Kafka
brew install kafka

# Install Spark
brew install apache-spark

# Install PostgreSQL
brew install postgresql@15

# Install Docker
brew install --cask docker

# Verify installations
java -version
mvn -version
scala -version
sbt -version
kafka-topics --version
spark-submit --version
psql --version
docker --version
```

#### Ubuntu/Debian

```bash
# Update package list
sudo apt update && sudo apt upgrade -y

# Install Java
sudo apt install openjdk-17-jdk -y

# Install Maven
sudo apt install maven -y

# Install Scala
sudo apt install scala -y

# Install sbt
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt update
sudo apt install sbt -y

# Install Kafka
wget https://downloads.apache.org/kafka/3.6.0/kafka_2.13-3.6.0.tgz
tar -xzf kafka_2.13-3.6.0.tgz
sudo mv kafka_2.13-3.6.0 /opt/kafka

# Install Spark
wget https://downloads.apache.org/spark/spark-3.5.0/spark-3.5.0-bin-hadoop3.tgz
tar -xzf spark-3.5.0-bin-hadoop3.tgz
sudo mv spark-3.5.0-bin-hadoop3 /opt/spark

# Install PostgreSQL
sudo apt install postgresql-15 postgresql-contrib -y

# Install Docker
sudo apt install docker.io docker-compose -y
sudo usermod -aG docker $USER

# Add to PATH
echo 'export KAFKA_HOME=/opt/kafka' >> ~/.bashrc
echo 'export SPARK_HOME=/opt/spark' >> ~/.bashrc
echo 'export PATH=$PATH:$KAFKA_HOME/bin:$SPARK_HOME/bin' >> ~/.bashrc
source ~/.bashrc
```

### Step 2: Install TimescaleDB Extension

```bash
# For Ubuntu/Debian
sudo sh -c "echo 'deb https://packagecloud.io/timescale/timescaledb/ubuntu/ $(lsb_release -c -s) main' > /etc/apt/sources.list.d/timescaledb.list"
wget --quiet -O - https://packagecloud.io/timescale/timescaledb/gpgkey | sudo apt-key add -
sudo apt update
sudo apt install timescaledb-2-postgresql-15 -y

# Configure TimescaleDB
sudo timescaledb-tune

# Restart PostgreSQL
sudo systemctl restart postgresql

# For macOS (using Homebrew)
brew install timescaledb

# Follow the post-install instructions to configure
```

### Step 3: Clone and Setup Project

```bash
# Create project directory
mkdir ~/stock-monitor-system
cd ~/stock-monitor-system

# Create project structure
mkdir -p java-producer/src/main/java/com/stockmonitor/producer
mkdir -p spark-processor/src/main/scala/com/stockmonitor/spark
mkdir -p dashboard
mkdir -p scripts
mkdir -p config

# Initialize Git (optional)
git init
```

### Step 4: Create Configuration Files

Create all the files from the previous implementation in their respective directories.

### Step 5: Setup Massive.com API Key

```bash
# Sign up at https://massive.com (formerly polygon.io)
# Get your API key from the dashboard

# Edit the Java producer file
nano java-producer/src/main/java/com/stockmonitor/producer/MassiveWebSocketProducer.java

# Replace YOUR_MASSIVE_API_KEY with your actual key
```

### Step 6: Build Projects

```bash
# Build Java Producer
cd java-producer
mvn clean package
cd ..

# Build Spark Processor
cd spark-processor
sbt clean package
cd ..
```

### Step 7: Initialize Database

```bash
# Start PostgreSQL
sudo systemctl start postgresql  # Linux
brew services start postgresql   # macOS

# Create database and tables
psql -U postgres -f init_db.sql

# Verify tables
psql -U postgres -d stockdb -c "\dt"
```

### Step 8: Start Services with Docker

```bash
# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# View logs
docker-compose logs -f
```

---

## Component Documentation

### 1. Java WebSocket Producer

#### Overview
The Java Producer is responsible for establishing a WebSocket connection to Massive.com, receiving real-time market data, and publishing it to Kafka.

#### Key Classes

**MassiveWebSocketProducer.java**
```
Main class that orchestrates the WebSocket connection and Kafka publishing.

Key Methods:
- createKafkaProducer(): Initializes Kafka producer with optimized settings
- start(): Establishes WebSocket connection
- processTradeData(): Enriches and publishes messages to Kafka
- shutdown(): Graceful shutdown with resource cleanup
```

#### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| MASSIVE_WS_URL | wss://socket.massive.com/stocks | WebSocket endpoint |
| KAFKA_BOOTSTRAP | localhost:9092 | Kafka broker address |
| KAFKA_TOPIC | raw-trades | Target Kafka topic |
| ACKS_CONFIG | 1 | Acknowledgment level |
| COMPRESSION_TYPE | snappy | Message compression |
| LINGER_MS | 10 | Batch delay |
| BATCH_SIZE | 32768 | Batch size in bytes |

#### Message Format

**Input (from Massive.com):**
```json
[
  {
    "ev": "A",
    "sym": "AAPL",
    "c": 175.50,
    "v": 125000,
    "t": 1704931200000
  }
]
```

**Output (to Kafka):**
```json
{
  "symbol": "AAPL",
  "price": 175.50,
  "volume": 125000,
  "timestamp": 1704931200000,
  "raw": "{...original message...}"
}
```

#### Error Handling

- **Connection Loss**: Automatic reconnection with exponential backoff
- **Kafka Unavailable**: Message buffering up to 10 seconds
- **Parsing Errors**: Logged and skipped (no pipeline interruption)
- **Rate Limiting**: Respect Massive.com API limits (5 calls/min on free tier)

#### Monitoring

```bash
# View producer logs
tail -f logs/producer.log

# Check Kafka topic
kafka-console-consumer --bootstrap-server localhost:9092 --topic raw-trades --from-beginning

# Monitor producer metrics
jconsole  # Connect to producer JVM process
```

---

### 2. Apache Kafka

#### Topic Configuration

**raw-trades**
```
Partitions: 3
Replication Factor: 1
Retention: 7 days (168 hours)
Compression: Producer-side (snappy)
Min In-Sync Replicas: 1
```

#### Partition Strategy

Messages are partitioned by stock symbol to ensure:
- All messages for AAPL go to the same partition
- Ordered processing per symbol
- Parallel processing across symbols

#### Consumer Groups

| Group ID | Purpose | Offset Management |
|----------|---------|-------------------|
| spark-streaming-group | Spark processing | Auto-commit (10s) |
| monitoring-group | Health checks | Manual commit |

#### Performance Tuning

```properties
# server.properties optimizations
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000
```

#### Commands Reference

```bash
# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
kafka-topics --bootstrap-server localhost:9092 --describe --topic raw-trades

# Consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group spark-streaming-group

# Delete topic (careful!)
kafka-topics --bootstrap-server localhost:9092 --delete --topic raw-trades

# Increase partitions
kafka-topics --bootstrap-server localhost:9092 --alter --topic raw-trades --partitions 6
```

---

### 3. Spark Streaming Processor

#### Overview
Spark Structured Streaming consumes from Kafka, performs windowed aggregations, calculates moving averages, and detects breakouts.

#### Key Concepts

**Watermarking**
```scala
.withWatermark("timestamp", "1 minute")
```
- Allows late data up to 1 minute
- Balances completeness vs latency
- Critical for financial accuracy

**Windowing**
```scala
window($"timestamp", "5 minutes", "1 minute")
```
- Window size: 5 minutes
- Slide interval: 1 minute
- Updates every minute with overlapping data

**Aggregations**
- avg(price): Moving average
- max(price): High in window
- min(price): Low in window
- sum(volume): Total volume
- count(*): Number of trades

#### Processing Flow

```
Kafka Source
    ↓
Parse JSON → Case Class
    ↓
Apply Watermark (1 min)
    ↓
Group by Symbol + Window (5 min / 1 min)
    ↓
Aggregate (avg, max, min, sum, count)
    ↓
Calculate MA
    ↓
Detect Breakouts
    ↓
Write to Database (JDBC)
    ↓
Write to Console (Monitoring)
```

#### Checkpointing

Spark maintains processing state in `/tmp/checkpoint`:
- Offset tracking
- Aggregation state
- Watermark information

**Important:** In production, use HDFS or S3 for checkpoints.

#### Triggers

```scala
.trigger(Trigger.ProcessingTime("10 seconds"))
```
- Micro-batches every 10 seconds
- Trade-off between latency and throughput
- Can be adjusted: 5s (lower latency) or 30s (higher throughput)

#### Output Modes

- **Append**: New results only (used for aggregations)
- **Update**: Changed results (efficient for dashboards)
- **Complete**: All results (memory intensive)

#### Performance Tuning

```scala
// Optimize shuffle partitions
.config("spark.sql.shuffle.partitions", "3")

// Memory management
.config("spark.executor.memory", "4g")
.config("spark.driver.memory", "2g")

// Checkpoint interval
.config("spark.sql.streaming.checkpointLocation.compactInterval", "10")
```

---

### 4. TimescaleDB / PostgreSQL

#### Schema Design

**stock_aggregates (Hypertable)**
```sql
Columns:
- id: SERIAL PRIMARY KEY
- symbol: VARCHAR(20)
- window_start: TIMESTAMP (partition key)
- window_end: TIMESTAMP
- avg_price: DOUBLE PRECISION
- max_price: DOUBLE PRECISION
- min_price: DOUBLE PRECISION
- total_volume: BIGINT
- trade_count: BIGINT
- ma_5min: DOUBLE PRECISION
- created_at: TIMESTAMP

Indexes:
- idx_stock_symbol: (symbol, window_start DESC)
- idx_stock_time: (window_start DESC)

Partitioning: By time (window_start) - automatic with hypertable
Chunk Interval: 7 days (default)
```

**breakout_alerts**
```sql
Columns:
- id: SERIAL PRIMARY KEY
- symbol: VARCHAR(20)
- timestamp: TIMESTAMP
- current_price: DOUBLE PRECISION
- window_avg: DOUBLE PRECISION
- signal: VARCHAR(10) -- BULLISH, BEARISH
- created_at: TIMESTAMP

Indexes:
- idx_alerts_symbol: (symbol, timestamp DESC)
- idx_alerts_time: (timestamp DESC)
```

#### TimescaleDB Features

**Continuous Aggregates**
```sql
CREATE MATERIALIZED VIEW stock_1hour_agg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', window_start) AS bucket,
    symbol,
    AVG(avg_price) as avg_price,
    MAX(max_price) as max_price,
    MIN(min_price) as min_price,
    SUM(total_volume) as total_volume
FROM stock_aggregates
GROUP BY bucket, symbol;
```

**Retention Policies**
```sql
-- Keep raw data for 30 days
SELECT add_retention_policy('stock_aggregates', INTERVAL '30 days');

-- Keep aggregates for 1 year
SELECT add_retention_policy('stock_1hour_agg', INTERVAL '365 days');
```

**Compression**
```sql
-- Enable compression on older data
ALTER TABLE stock_aggregates SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'symbol'
);

-- Compress data older than 7 days
SELECT add_compression_policy('stock_aggregates', INTERVAL '7 days');
```

#### Queries

**Get Latest Prices**
```sql
SELECT DISTINCT ON (symbol)
    symbol,
    window_start,
    avg_price,
    ma_5min,
    total_volume
FROM stock_aggregates
ORDER BY symbol, window_start DESC;
```

**Detect Golden Cross**
```sql
SELECT * FROM get_golden_crosses('AAPL', 24);
```

**Price History**
```sql
SELECT 
    window_start,
    avg_price,
    ma_5min
FROM stock_aggregates
WHERE symbol = 'AAPL'
  AND window_start > NOW() - INTERVAL '1 day'
ORDER BY window_start;
```

#### Backup & Restore

```bash
# Backup
pg_dump -U postgres -d stockdb -F c -f stockdb_backup.dump

# Restore
pg_restore -U postgres -d stockdb -c stockdb_backup.dump

# Automated backup (cron)
0 2 * * * pg_dump -U postgres -d stockdb -F c -f /backups/stockdb_$(date +\%Y\%m\%d).dump
```

---

### 5. Grafana Dashboards

#### Dashboard Components

**1. Live Stock Prices (Time Series)**
- X-axis: Time
- Y-axis: Price
- Series: AAPL, TSLA, BTC
- Refresh: 10s

**2. Price vs MA (Time Series)**
- Dual Y-axis
- Line: Current Price
- Line (dashed): 5-min MA
- Fill area between lines
- Variable: $symbol

**3. Trading Volume (Bar Chart)**
- X-axis: Time
- Y-axis: Volume
- Color: By symbol

**4. Breakout Alerts (Table)**
- Columns: Timestamp, Symbol, Signal, Price, MA
- Sort: Timestamp DESC
- Limit: 50 rows
- Auto-refresh: 10s

**5. Current Trend (Stat)**
- Single value panels per symbol
- Color: Green (bullish), Red (bearish)
- Shows: Price, MA, Trend

#### Alert Rules

**Golden Cross Alert**
```yaml
Condition: avg_price > ma_5min (crossing from below)
Frequency: 30s
Notification: Email, Slack
Message: "🚀 {{ $labels.symbol }}: Price crossed above MA at ${{ $value }}"
```

**Death Cross Alert**
```yaml
Condition: avg_price < ma_5min (crossing from above)
Frequency: 30s
Notification: Email, Slack
Message: "📉 {{ $labels.symbol }}: Price crossed below MA at ${{ $value }}"
```

**High Volume Alert**
```yaml
Condition: total_volume > 1000000
Frequency: 1m
Notification: Email
Message: "⚡ {{ $labels.symbol }}: Unusual volume detected: {{ $value }}"
```

#### Notification Channels

**Email Configuration**
```ini
[smtp]
enabled = true
host = smtp.gmail.com:587
user = your-email@gmail.com
password = your-app-password
from_address = alerts@stockmonitor.com
from_name = Stock Monitor Alerts
```

**Slack Configuration**
```bash
# Create incoming webhook in Slack
# Add to Grafana: Configuration > Notification channels > New channel
Type: Slack
Webhook URL: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

#### Variables

```
$symbol: AAPL, TSLA, BTC (multi-select)
$timerange: Last 1h, Last 6h, Last 24h, Last 7d
$interval: 1m, 5m, 15m, 1h
```

---

## Configuration Guide

### Environment Variables

Create `.env` file:
```bash
# Massive.com API
MASSIVE_API_KEY=your_api_key_here
MASSIVE_WS_URL=wss://socket.massive.com/stocks

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_RAW_TRADES=raw-trades
KAFKA_PARTITIONS=3
KAFKA_REPLICATION_FACTOR=1

# Spark
SPARK_MASTER=local[*]
SPARK_CHECKPOINT_DIR=/tmp/checkpoint
SPARK_SHUFFLE_PARTITIONS=3

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=stockdb
DB_USER=postgres
DB_PASSWORD=postgres

# Grafana
GRAFANA_PORT=3000
GRAFANA_ADMIN_PASSWORD=admin

# Application
LOG_LEVEL=INFO
TRIGGER_INTERVAL=10s
WATERMARK_DELAY=1m
WINDOW_DURATION=5m
SLIDE_DURATION=1m
```

### Java Producer Configuration

**application.properties**
```properties
# WebSocket
websocket.url=${MASSIVE_WS_URL}
websocket.api.key=${MASSIVE_API_KEY}
websocket.reconnect.delay=5000
websocket.max.retries=3

# Kafka Producer
kafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS}
kafka.topic=${KAFKA_TOPIC_RAW_TRADES}
kafka.acks=1
kafka.retries=3
kafka.compression.type=snappy
kafka.linger.ms=10
kafka.batch.size=32768
kafka.buffer.memory=33554432

# Logging
logging.level=${LOG_LEVEL}
logging.file=logs/producer.log
```

### Spark Configuration

**spark-defaults.conf**
```properties
spark.master                     ${SPARK_MASTER}
spark.executor.memory            4g
spark.driver.memory              2g
spark.sql.shuffle.partitions     ${SPARK_SHUFFLE_PARTITIONS}
spark.streaming.stopGracefullyOnShutdown    true
spark.sql.streaming.checkpointLocation      ${SPARK_CHECKPOINT_DIR}
spark.sql.adaptive.enabled       true
spark.sql.adaptive.coalescePartitions.enabled    true
```

### Database Configuration

**postgresql.conf**
```conf
# Connection Settings
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 16MB
maintenance_work_mem = 128MB

# WAL Settings
wal_level = replica
max_wal_size = 1GB
min_wal_size = 80MB

# TimescaleDB
shared_preload_libraries = 'timescaledb'
timescaledb.max_background_workers = 8
```

---

## Deployment

### Development Environment

```bash
# Start all services locally
./start_pipeline.sh

# Access services
# Kafka: localhost:9092
# Database: localhost:5432
# Grafana: http://localhost:3000
# Spark UI: http://localhost:4040
```

### Staging Environment

```bash
# Use Docker Compose with staging config
docker-compose -f docker-compose.staging.yml up -d

# Deploy to staging server
scp -r stock-monitor-system user@staging-server:/opt/
ssh user@staging-server
cd /opt/stock-monitor-system
./start_pipeline.sh
```

### Production Deployment

#### Option 1: Kubernetes (Recommended)

**kafka-deployment.yaml**
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
spec:
  serviceName: kafka
  replicas: 3
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_BROKER_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: "zookeeper:2181"
        volumeMounts:
        - name: kafka-storage
          mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
  - metadata:
      name: kafka-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 100Gi
```

**spark-deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spark-streaming
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spark-streaming
  template:
    metadata:
      labels:
        app: spark-streaming
    spec:
      containers:
      - name: spark
        image: bitnami/spark:3.5.0
        command: ["/opt/spark/bin/spark-submit"]
        args:
          - "--class"
          - "com.stockmonitor.spark.SparkStockProcessor"
          - "--master"
          - "k8s://https://kubernetes.default.svc"
          - "/app/stock-monitor-spark_2.12-1.0.jar"
        env:
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        volumeMounts:
        - name: checkpoint
          mountPath: /checkpoint
      volumes:
      - name: checkpoint
        persistentVolumeClaim:
          claimName: spark-checkpoint-pvc
```

#### Option 2: AWS Deployment

**Infrastructure as Code (Terraform)**
```hcl
# main.tf

provider "aws" {
  region = "us-east-1"
}

# MSK (Managed Kafka)
resource "aws_msk_cluster" "stock_monitor" {
  cluster_name           = "stock-monitor-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = aws_subnet.private[*].id
    security_groups = [aws_security_group.kafka.id]
  }
}

# RDS PostgreSQL with TimescaleDB
resource "aws_db_instance" "timescaledb" {
  identifier           = "stock-monitor-db"
  engine              = "postgres"
  engine_version      = "15.4"
  instance_class      = "db.r6g.xlarge"
  allocated_storage   = 500
  storage_type        = "gp3"
  db_name             = "stockdb"
  username            = "postgres"
  password            = var.db_password
  publicly_accessible = false
  vpc_security_group_ids = [aws_security_group.db.id]
}

# EMR for Spark
resource "aws_emr_cluster" "spark" {
  name          = "stock-monitor-spark"
  release_label = "emr-6.14.0"
  applications  = ["Spark", "Hadoop"]

  ec2_attributes {
    subnet_id                         = aws_subnet.private[0].id
    emr_managed_master_security_group = aws_security_group.emr_master.id
    emr_managed_slave_security_group  = aws_security_group.emr_slave.id
    instance_profile                  = aws_iam_instance_profile.emr.arn
  }

  master_instance_group {
    instance_type = "m5.xlarge"
  }

  core_instance_group {
    instance_type  = "m5.xlarge"
    instance_count = 2
  }
}
```

#### Option 3: Docker Swarm

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.prod.yml stock-monitor

# Scale services
docker service scale stock-monitor_spark=3

# Update service
docker service update --image bitnami/spark:3.5.1 stock-monitor_spark
```

### CI/CD Pipeline

**GitHub Actions (.github/workflows/deploy.yml)**
```yaml
name: Deploy Stock Monitor

on:
  push:
    branches: [ main ]

jobs:
  build-java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Maven
        run: |
          cd java-producer
          mvn clean package
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: producer-jar
          path: java-producer/target/*.jar

  build-scala:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Scala
        uses: olafurpg/setup-scala@v13
      - name: Build with sbt
        run: |
          cd spark-processor
          sbt clean package
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: spark-jar
          path: spark-processor/target/scala-2.12/*.jar

  deploy:
    needs: [build-java, build-scala]
    runs-on: ubuntu-latest
    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v3
      - name: Deploy to server
        run: |
          scp producer-jar/*.jar user@prod-server:/opt/stock-monitor/
          scp spark-jar/*.jar user@prod-server:/opt/stock-monitor/
          ssh user@prod-server './restart_pipeline.sh'
```

---

## Monitoring & Troubleshooting

### Health Checks

**System Health Dashboard**
```bash
#!/bin/bash
# health_check.sh

echo "=== Stock Monitor Health Check ==="
echo ""

# Check Kafka
echo "1. Kafka Status:"
kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ Kafka is running"
else
    echo "   ✗ Kafka is DOWN"
fi

# Check Database
echo "2. Database Status:"
psql -U postgres -d stockdb -c "SELECT 1" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ Database is running"
else
    echo "   ✗ Database is DOWN"
fi

# Check record count
echo "3. Data Status:"
RECORD_COUNT=$(psql -U postgres -d stockdb -t -c "SELECT COUNT(*) FROM stock_aggregates WHERE window_start > NOW() - INTERVAL '1 hour'")
echo "   Records (last hour): $RECORD_COUNT"

# Check Spark
echo "4. Spark Status:"
if pgrep -f "SparkStockProcessor" > /dev/null; then
    echo "   ✓ Spark is running"
else
    echo "   ✗ Spark is DOWN"
fi

# Check Producer
echo "5. Producer Status:"
if pgrep -f "MassiveWebSocketProducer" > /dev/null; then
    echo "   ✓ Producer is running"
else
    echo "   ✗ Producer is DOWN"
fi

# Check Grafana
echo "6. Grafana Status:"
curl -s http://localhost:3000/api/health > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✓ Grafana is running"
else
    echo "   ✗ Grafana is DOWN"
fi
```

### Log Management

**Centralized Logging with ELK Stack**
```yaml
# docker-compose.elk.yml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.10.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5000:5000"

  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
```

**logstash.conf**
```conf
input {
  file {
    path => "/logs/producer.log"
    type => "producer"
  }
  file {
    path => "/logs/spark.log"
    type => "spark"
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "stock-monitor-%{+YYYY.MM.dd}"
  }
}
```

### Common Issues & Solutions

#### Issue 1: Kafka Connection Timeout

**Symptoms:**
```
ERROR: Connection to node -1 could not be established
```

**Solution:**
```bash
# Check Kafka is running
sudo systemctl status kafka

# Check listeners
grep "listeners=" /opt/kafka/config/server.properties

# Should be: listeners=PLAINTEXT://localhost:9092

# Restart Kafka
sudo systemctl restart kafka
```

#### Issue 2: Spark Checkpointing Errors

**Symptoms:**
```
ERROR: Checkpoint directory /tmp/checkpoint not found
```

**Solution:**
```bash
# Create checkpoint directory
mkdir -p /tmp/checkpoint
chmod 777 /tmp/checkpoint

# Or use HDFS in production
hdfs dfs -mkdir -p /spark/checkpoint
```

#### Issue 3: Database Connection Pool Exhausted

**Symptoms:**
```
ERROR: FATAL: sorry, too many clients already
```

**Solution:**
```sql
-- Increase max connections
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();

-- Check current connections
SELECT count(*) FROM pg_stat_activity;

-- Kill idle connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'idle' 
AND state_change < NOW() - INTERVAL '10 minutes';
```

#### Issue 4: Out of Memory (Spark)

**Symptoms:**
```
ERROR: Java heap space
```

**Solution:**
```bash
# Increase executor memory
spark-submit \
  --executor-memory 8g \
  --driver-memory 4g \
  --conf spark.memory.fraction=0.8 \
  ...

# Monitor memory usage
jstat -gc <spark-pid> 1000
```

#### Issue 5: Watermark Not Working

**Symptoms:**
- Late data not being processed
- Windows not closing

**Solution:**
```scala
// Increase watermark delay
.withWatermark("timestamp", "5 minutes")  // Instead of 1 minute

// Check for clock skew
// Ensure all systems are NTP synchronized
```

### Performance Metrics

**Kafka Metrics**
```bash
# Producer throughput
kafka-run-class kafka.tools.JmxTool \
  --object-name kafka.producer:type=producer-metrics,client-id=* \
  --attributes record-send-rate

# Consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group spark-streaming-group
```

**Spark Metrics**
```scala
// Add to SparkStockProcessor
val listener = new StreamingQueryListener {
  override def onQueryProgress(event: QueryProgressEvent): Unit = {
    println(s"Batch: ${event.progress.batchId}")
    println(s"Input Rows: ${event.progress.numInputRows}")
    println(s"Processing Time: ${event.progress.durationMs}")
  }
}
spark.streams.addListener(listener)
```

**Database Metrics**
```sql
-- Query performance
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;

-- Table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## Performance Tuning

### Kafka Tuning

**Producer Optimization**
```properties
# Increase batch size for higher throughput
batch.size=65536
linger.ms=20

# Use snappy compression
compression.type=snappy

# Async sends with callbacks
acks=1
max.in.flight.requests.per.connection=5
```

**Broker Optimization**
```properties
# Increase network threads
num.network.threads=16
num.io.threads=16

# Larger buffers
socket.send.buffer.bytes=1048576
socket.receive.buffer.bytes=1048576

# Compression
compression.type=snappy
```

### Spark Tuning

**Memory Management**
```scala
spark.executor.memory=8g
spark.driver.memory=4g
spark.memory.fraction=0.8
spark.memory.storageFraction=0.3
```

**Shuffle Optimization**
```scala
spark.sql.shuffle.partitions=6  // 2x number of cores
spark.sql.adaptive.enabled=true
spark.sql.adaptive.coalescePartitions.enabled=true
```

**Checkpoint Optimization**
```scala
spark.sql.streaming.checkpointLocation=/hdfs/checkpoint
spark.cleaner.referenceTracking.cleanCheckpoints=true
```

### Database Tuning

**Connection Pooling**
```properties
# HikariCP configuration
maximumPoolSize=20
minimumIdle=5
connectionTimeout=30000
idleTimeout=600000
maxLifetime=1800000
```

**Index Optimization**
```sql
-- Analyze tables regularly
ANALYZE stock_aggregates;

-- Reindex periodically
REINDEX TABLE stock_aggregates;

-- Vacuum
VACUUM ANALYZE stock_aggregates;
```

**Query Optimization**
```sql
-- Use prepared statements
PREPARE get_latest AS
SELECT * FROM stock_aggregates 
WHERE symbol = $1 
ORDER BY window_start DESC 
LIMIT 1;

EXECUTE get_latest('AAPL');
```

### Network Tuning

**Linux Kernel Parameters**
```bash
# /etc/sysctl.conf
net.core.rmem_max=134217728
net.core.wmem_max=134217728
net.ipv4.tcp_rmem=4096 87380 67108864
net.ipv4.tcp_wmem=4096 65536 67108864
net.core.netdev_max_backlog=5000
```

---

## API Reference

### REST API (Future Enhancement)

**Endpoint: Get Latest Price**
```
GET /api/v1/stocks/{symbol}/latest
Response:
{
  "symbol": "AAPL",
  "price": 175.50,
  "ma_5min": 175.00,
  "volume": 125000,
  "trend": "bullish",
  "timestamp": "2026-01-17T10:30:00Z"
}
```

**Endpoint: Get Historical Data**
```
GET /api/v1/stocks/{symbol}/history?from=2026-01-17T00:00:00Z&to=2026-01-17T23:59:59Z
Response:
{
  "symbol": "AAPL",
  "data": [
    {
      "timestamp": "2026-01-17T10:00:00Z",
      "price": 175.00,
      "volume": 100000
    },
    ...
  ]
}
```

**Endpoint: Get Alerts**
```
GET /api/v1/alerts?symbol=AAPL&type=golden_cross&limit=10
Response:
{
  "alerts": [
    {
      "symbol": "AAPL",
      "type": "golden_cross",
      "price": 175.50,
      "timestamp": "2026-01-17T10:30:00Z"
    },
    ...
  ]
}
```

---

## FAQ

**Q: How much does Massive.com API cost?**
A: Free tier: 5 API calls/min with delayed data. Starter: $99/month for real-time. Pro: $399/month unlimited.

**Q: Can I add more stocks?**
A: Yes, modify the subscription in `MassiveWebSocketProducer.java`:
```java
String subscribeMessage = 
    "{\"action\":\"subscribe\",\"params\":\"A.AAPL,A.TSLA,A.GOOGL,A.AMZN,A.X:BTCUSD\"}";
```

**Q: How do I change the window size?**
A: Modify in `SparkStockProcessor.scala`:
```scala
window($"timestamp", "10 minutes", "2 minutes")  // 10-min window, 2-min slide
```

**Q: What's the data retention period?**
A: Default: 30 days. Change in TimescaleDB:
```sql
SELECT add_retention_policy('stock_aggregates', INTERVAL '90 days');
```

**Q: How to backup the system?**
A: Use the backup script:
```bash
./scripts/backup_system.sh
```

**Q: Can I run this on Windows?**
A: Yes, using WSL2 (Windows Subsystem for Linux) or Docker Desktop.

**Q: What's the expected latency?**
A: End-to-end: 2-5 seconds (WebSocket → Kafka → Spark → DB → Grafana).

**Q: How to scale for more symbols?**
A: Increase Kafka partitions, Spark executors, and database connections proportionally.

**Q: Is this production-ready?**
A: This is a foundation. For production, add: authentication, SSL/TLS, monitoring, alerting, high availability, disaster recovery.

**Q: How to contribute?**
A: Fork the repository, create a feature branch, submit a pull request with tests.

---

## Conclusion

This system demonstrates a complete real-time data pipeline using modern technologies. It's designed to be educational, extensible, and production-ready with proper tuning and security hardening.

For questions or issues, please open a GitHub issue or contact the maintainers.

**Happy Trading!**
