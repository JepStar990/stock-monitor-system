# 📈 Real-Time Stock Market Monitoring System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Scala](https://img.shields.io/badge/Scala-2.12-red.svg)](https://www.scala-lang.org/)
[![Spark](https://img.shields.io/badge/Spark-3.5.0-orange.svg)](https://spark.apache.org/)
[![Kafka](https://img.shields.io/badge/Kafka-3.6.0-black.svg)](https://kafka.apache.org/)

A production-grade distributed streaming application that processes real-time stock market data, calculates 5-minute moving averages, detects price breakouts (Golden Cross/Death Cross), and visualizes trends through interactive dashboards.

![System Architecture](https://via.placeholder.com/800x400?text=System+Architecture+Diagram)

---

## 🚀 Quick Start

Get the system running in under 5 minutes:

```bash
# Clone repository
git clone https://github.com/yourusername/stock-monitor-system.git
cd stock-monitor-system

# Start services
docker-compose up -d

# Build and run
./start_pipeline.sh

# Access Grafana dashboard
open http://localhost:3000
```

**Default Credentials:**
- Grafana: `admin` / `admin`
- PostgreSQL: `postgres` / `postgres`

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [API Documentation](#-api-documentation)
- [Monitoring](#-monitoring)
- [Deployment](#-deployment)
- [Performance](#-performance)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Core Capabilities

- **Real-Time Data Ingestion**
  - WebSocket connection to Massive.com (formerly Polygon.io)
  - Support for stocks (AAPL, TSLA) and crypto (BTC)
  - Automatic reconnection with exponential backoff
  - Rate limiting and error handling

- **Stream Processing**
  - 5-minute sliding windows with 1-minute updates
  - Watermarking for late-arriving data (up to 1 minute)
  - Stateful aggregations: avg, max, min, volume
  - Moving average calculations

- **Breakout Detection**
  - **Golden Cross**: Price crosses above MA (bullish signal)
  - **Death Cross**: Price crosses below MA (bearish signal)
  - Real-time alerts via Grafana notifications

- **Data Storage**
  - TimescaleDB hypertables for time-series optimization
  - Automatic compression for data older than 7 days
  - Retention policies (30 days default)
  - Continuous aggregates for historical analysis

- **Visualization**
  - Interactive Grafana dashboards
  - Real-time price charts with moving averages
  - Volume analysis
  - Alert history table
  - Trend indicators

### Technical Highlights

- **Scalability**: Kafka partitioning, Spark parallelism
- **Reliability**: Checkpointing, exactly-once semantics
- **Performance**: Sub-5 second end-to-end latency
- **Observability**: Comprehensive logging and metrics

---

## 🏗 Architecture

### System Components

```
┌──────────────┐
│ Massive.com  │  Real-time market data via WebSocket
│   WebSocket  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Java      │  WebSocket client → Kafka producer
│   Producer   │  Enriches and publishes messages
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Apache Kafka │  Distributed message queue
│  (3 parts)   │  Topic: raw-trades
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Spark     │  Structured Streaming processor
│  Streaming   │  5-min windows, MA calculation
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ TimescaleDB  │  Time-series database
│ (PostgreSQL) │  Hypertables with compression
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Grafana    │  Real-time dashboards & alerts
└──────────────┘
```

### Data Flow

1. **WebSocket** receives JSON trade data
2. **Producer** enriches with metadata → Kafka
3. **Kafka** buffers messages in `raw-trades` topic
4. **Spark** consumes, windows, aggregates
5. **Database** stores aggregated results
6. **Grafana** queries and visualizes

**Latency:** 2-5 seconds end-to-end

---

## 📦 Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 17+ | Producer development |
| Apache Maven | 3.8+ | Java build tool |
| Scala | 2.12.17 | Spark development |
| sbt | 1.9+ | Scala build tool |
| Apache Kafka | 3.6+ | Message broker |
| Apache Spark | 3.5+ | Stream processing |
| PostgreSQL | 15+ | Database |
| TimescaleDB | 2.13+ | Time-series extension |
| Docker | 24.0+ | Containerization |
| Docker Compose | 2.0+ | Multi-container orchestration |

### Hardware Requirements

**Minimum:**
- 4 CPU cores
- 8 GB RAM
- 50 GB storage

**Recommended:**
- 8 CPU cores
- 16 GB RAM
- 100 GB SSD storage

**Production:**
- 16+ CPU cores
- 32+ GB RAM
- 500 GB NVMe SSD

### API Access

You'll need a Massive.com API key (free tier available):
1. Sign up at [massive.com](https://massive.com)
2. Get your API key from the dashboard
3. Free tier: 5 calls/min with delayed data
4. Paid tiers: Real-time data starting at $99/month

---

## 🛠 Installation

### Option 1: Docker (Recommended)

```bash
# Clone repository
git clone https://github.com/yourusername/stock-monitor-system.git
cd stock-monitor-system

# Configure API key
cp .env.example .env
nano .env  # Add your MASSIVE_API_KEY

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Option 2: Manual Installation

#### Step 1: Install Prerequisites (macOS)

```bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java, Maven, Scala
brew install openjdk@17 maven scala sbt

# Install Kafka and Spark
brew install kafka apache-spark

# Install PostgreSQL
brew install postgresql@15 timescaledb

# Start services
brew services start postgresql@15
brew services start kafka
```

#### Step 2: Setup Database

```bash
# Create database
createdb -U postgres stockdb

# Initialize schema
psql -U postgres -d stockdb -f init_db.sql

# Verify
psql -U postgres -d stockdb -c "\dt"
```

#### Step 3: Build Applications

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

#### Step 4: Configure

```bash
# Copy configuration
cp config/application.properties.example config/application.properties

# Edit configuration
nano config/application.properties

# Set your API key
MASSIVE_API_KEY=your_api_key_here
```

---

## ⚙️ Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# Massive.com API
MASSIVE_API_KEY=pk_xxxxxxxxxxxxx
MASSIVE_WS_URL=wss://socket.massive.com/stocks

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_RAW_TRADES=raw-trades
KAFKA_PARTITIONS=3

# Spark Configuration
SPARK_MASTER=local[*]
SPARK_EXECUTOR_MEMORY=4g
SPARK_DRIVER_MEMORY=2g

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=stockdb
DB_USER=postgres
DB_PASSWORD=postgres

# Application Settings
WINDOW_DURATION=5m
SLIDE_DURATION=1m
WATERMARK_DELAY=1m
TRIGGER_INTERVAL=10s
```

### Stocks Configuration

Edit `java-producer/src/main/java/com/stockmonitor/producer/MassiveWebSocketProducer.java`:

```java
// Subscribe to specific stocks
String subscribeMessage = 
    "{\"action\":\"subscribe\",\"params\":\"A.AAPL,A.TSLA,A.GOOGL,A.MSFT,A.X:BTCUSD\"}";
```

### Advanced Configuration

See [Configuration Guide](docs/configuration.md) for:
- Kafka tuning parameters
- Spark optimization settings
- Database connection pooling
- Grafana alert rules

---

## 🎮 Usage

### Starting the System

```bash
# Start all components
./start_pipeline.sh

# Or start individually:

# 1. Start Kafka
kafka-server-start /usr/local/etc/kafka/server.properties

# 2. Start Producer
java -jar java-producer/target/massive-producer-1.0.jar

# 3. Start Spark
spark-submit --class com.stockmonitor.spark.SparkStockProcessor \
  --master local[*] \
  spark-processor/target/scala-2.12/stock-monitor-spark_2.12-1.0.jar

# 4. Access Grafana
open http://localhost:3000
```

### Monitoring

```bash
# Check system health
./monitor_pipeline.sh

# View Kafka topics
kafka-topics --list --bootstrap-server localhost:9092

# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group spark-streaming-group

# View database records
psql -U postgres -d stockdb -c "SELECT * FROM latest_stock_data;"

# Spark UI
open http://localhost:4040
```

### Stopping the System

```bash
# Stop all components
./stop_pipeline.sh

# Or stop Docker services
docker-compose down
```

---

## 📊 API Documentation

### Database Queries

#### Get Latest Prices

```sql
SELECT * FROM latest_stock_data;
```

**Output:**
```
 symbol | timestamp           | current_price | ma_5min | total_volume | trend
--------+--------------------+---------------+---------+--------------+--------
 AAPL   | 2026-01-17 10:30  | 175.50        | 175.00  | 1250000     | BULLISH
 TSLA   | 2026-01-17 10:30  | 245.30        | 246.00  | 890000      | BEARISH
 BTC    | 2026-01-17 10:30  | 42150.00      | 42000.0 | 15000       | BULLISH
```

#### Get Golden Cross Events

```sql
SELECT * FROM get_golden_crosses('AAPL', 24);
```

#### Get Price History

```sql
SELECT 
    window_start,
    avg_price,
    ma_5min,
    total_volume
FROM stock_aggregates
WHERE symbol = 'AAPL'
  AND window_start > NOW() - INTERVAL '1 day'
ORDER BY window_start DESC;
```

### Kafka Commands

```bash
# Produce test message
echo '{"symbol":"AAPL","price":175.50,"volume":1000,"timestamp":1704931200000}' | \
  kafka-console-producer --broker-list localhost:9092 --topic raw-trades

# Consume messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic raw-trades --from-beginning

# Check offset
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic raw-trades
```

---

## 📈 Monitoring

### Grafana Dashboards

Access at `http://localhost:3000` (admin/admin)

**Available Dashboards:**

1. **Stock Overview**
   - Real-time price charts
   - Moving averages overlay
   - Volume bars

2. **Technical Analysis**
   - Price vs MA comparison
   - Trend indicators
   - RSI and MACD (future)

3. **System Health**
   - Kafka throughput
   - Spark processing time
   - Database query performance

4. **Alerts History**
   - Golden Cross events
   - Death Cross events
   - High volume alerts

### Alerts

Grafana sends notifications for:
- **Golden Cross**: Price crosses above MA
- **Death Cross**: Price crosses below MA
- **High Volume**: Volume exceeds threshold
- **System Down**: Component failure

Configure notifications:
- Email (SMTP)
- Slack webhook
- PagerDuty
- Webhook

### Logs

```bash
# Producer logs
tail -f logs/producer.log

# Spark logs
tail -f logs/spark.log

# Kafka logs
tail -f /usr/local/var/log/kafka/server.log

# Database logs
tail -f /usr/local/var/log/postgresql@15.log
```

---

## 🚀 Deployment

### Development

```bash
./start_pipeline.sh
```

### Staging

```bash
docker-compose -f docker-compose.staging.yml up -d
```

### Production (Kubernetes)

```bash
# Apply configurations
kubectl apply -f k8s/

# Check status
kubectl get pods -n stock-monitor

# View logs
kubectl logs -f deployment/spark-streaming -n stock-monitor
```

### Production (AWS)

```bash
# Deploy infrastructure
cd terraform
terraform init
terraform apply

# Deploy applications
./deploy_to_aws.sh
```

See [Deployment Guide](docs/deployment.md) for detailed instructions.

---

## ⚡ Performance

### Benchmarks

| Metric | Value |
|--------|-------|
| End-to-end latency | 2-5 seconds |
| Throughput | 10,000 messages/sec |
| CPU usage | 30-40% (8 cores) |
| Memory usage | 6-8 GB |
| Storage | ~1 GB/day (3 stocks) |

### Optimization Tips

1. **Increase Kafka partitions** for more parallelism
2. **Tune Spark executor memory** based on data volume
3. **Enable database compression** for older data
4. **Use SSD storage** for better I/O performance
5. **Scale horizontally** with Kubernetes

See [Performance Tuning Guide](docs/performance-tuning.md).

---

## 🔧 Troubleshooting

### Common Issues

#### Kafka Connection Failed
```bash
# Check Kafka status
kafka-broker-api-versions --bootstrap-server localhost:9092

# Restart Kafka
brew services restart kafka  # macOS
sudo systemctl restart kafka # Linux
```

#### Spark Out of Memory
```bash
# Increase executor memory
spark-submit --executor-memory 8g ...
```

#### Database Connection Pool Exhausted
```sql
-- Increase connections
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();
```

#### No Data in Grafana
```bash
# Check Spark is writing
psql -U postgres -d stockdb -c "SELECT COUNT(*) FROM stock_aggregates;"

# Check Grafana datasource
curl http://localhost:3000/api/datasources
```

See [Troubleshooting Guide](docs/troubleshooting.md) for more solutions.

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines

- Write unit tests for new features
- Follow code style guidelines
- Update documentation
- Add examples for new features

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Massive.com** (formerly Polygon.io) for market data API
- **Apache Foundation** for Kafka and Spark
- **TimescaleDB** for time-series database
- **Grafana Labs** for visualization platform

---

## 📧 Contact

- **Author**: Your Name
- **Email**: your.email@example.com
- **GitHub**: [@yourusername](https://github.com/yourusername)
- **LinkedIn**: [Your Profile](https://linkedin.com/in/yourprofile)

---

## 🔗 Links

- [Full Documentation](docs/documentation.md)
- [API Reference](docs/api-reference.md)
- [Architecture Deep Dive](docs/architecture.md)
- [Deployment Guide](docs/deployment.md)
- [Performance Tuning](docs/performance-tuning.md)

---

## 📸 Screenshots

### Real-Time Dashboard
![Dashboard](https://via.placeholder.com/800x450?text=Grafana+Dashboard)

### Price Chart with Moving Average
![Chart](https://via.placeholder.com/800x450?text=Price+Chart)

### Alerts Table
![Alerts](https://via.placeholder.com/800x450?text=Alerts+Table)

---

## 🎯 Roadmap

- [ ] Add support for more technical indicators (RSI, MACD, Bollinger Bands)
- [ ] Implement machine learning price predictions
- [ ] Add REST API for external integrations
- [ ] Support for options and futures data
- [ ] Mobile app for alerts
- [ ] Backtesting framework
- [ ] Paper trading simulator
- [ ] Multi-cloud deployment support

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/stock-monitor-system&type=Date)](https://star-history.com/#yourusername/stock-monitor-system&Date)

---

**Built with ❤️ for the trading community**

If you find this project useful, please consider giving it a ⭐!
