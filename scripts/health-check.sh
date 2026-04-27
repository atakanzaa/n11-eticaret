#!/bin/bash
echo "Checking infrastructure health..."

check() {
    local name=$1
    local cmd=$2
    if eval "$cmd" > /dev/null 2>&1; then
        echo "✓ $name"
    else
        echo "✗ $name (FAILED)"
    fi
}

check "Postgres"    "docker exec smartcommerce-postgres pg_isready -U smartcommerce"
check "Redis"       "docker exec smartcommerce-redis redis-cli ping"
check "Kafka"       "docker exec smartcommerce-kafka kafka-topics --bootstrap-server localhost:9092 --list"
check "RabbitMQ"    "docker exec smartcommerce-rabbitmq rabbitmq-diagnostics ping"
check "OpenSearch"  "curl -s http://localhost:9200/_cluster/health"

echo "Done."
