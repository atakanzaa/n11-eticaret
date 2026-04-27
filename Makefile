.PHONY: help up down restart logs build test clean health create-topics

help:
	@echo "Available commands:"
	@echo "  make up           - Start all infrastructure"
	@echo "  make down         - Stop all infrastructure"
	@echo "  make restart      - Restart infrastructure"
	@echo "  make logs         - Tail logs"
	@echo "  make build        - Build all modules"
	@echo "  make test         - Run all tests"
	@echo "  make clean        - Clean all build artifacts"
	@echo "  make health       - Check service health"
	@echo "  make create-topics- Create Kafka topics"

up:
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 15
	@./scripts/health-check.sh

down:
	docker-compose down

restart: down up

logs:
	docker-compose logs -f

build:
	mvn clean install -DskipTests

test:
	mvn verify

clean:
	mvn clean
	docker-compose down -v

health:
	@./scripts/health-check.sh

create-topics:
	@./infra/kafka/create-topics.sh
