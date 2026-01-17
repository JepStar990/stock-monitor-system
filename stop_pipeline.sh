# stop_pipeline.sh

#!/bin/bash

echo "🛑 Stopping Stock Market Monitoring Pipeline..."

# Stop Java Producer
if [ -f .producer.pid ]; then
    kill $(cat .producer.pid)
    rm .producer.pid
fi

# Stop Spark
if [ -f .spark.pid ]; then
    kill $(cat .spark.pid)
    rm .spark.pid
fi

# Stop Docker services
docker-compose down

echo "✅ Pipeline stopped successfully!"
