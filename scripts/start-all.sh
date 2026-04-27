#!/bin/bash
set -e
echo "Starting SmartCommerce infrastructure..."
docker-compose up -d
echo "Infrastructure started. Run 'make create-topics' to create Kafka topics."
