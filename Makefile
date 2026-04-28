.PHONY: help up down restart logs build test clean health create-topics \
        build-services up-services down-services logs-services rebuild-service \
        up-observability down-observability up-all down-all health-services

help:
	@echo "── Infrastructure (postgres, redis, kafka, rabbitmq, opensearch) ──"
	@echo "  make up               - Start infra"
	@echo "  make down             - Stop infra"
	@echo "  make restart          - Restart infra"
	@echo "  make health           - Check infra health"
	@echo "  make create-topics    - Create Kafka topics"
	@echo ""
	@echo "── Application services (18 services) ──"
	@echo "  make build            - mvn clean install (produces JARs)"
	@echo "  make build-services   - mvn package + docker-compose build"
	@echo "  make up-services      - Start all 18 services"
	@echo "  make down-services    - Stop all 18 services"
	@echo "  make logs-services    - Tail service logs"
	@echo "  make rebuild-service SVC=order-service  - Rebuild + restart one service"
	@echo "  make health-services  - Curl /actuator/health on every service"
	@echo ""
	@echo "── Observability (prometheus, grafana, loki, tempo) ──"
	@echo "  make up-observability   - Start observability stack"
	@echo "  make down-observability - Stop observability stack"
	@echo ""
	@echo "── All-in-one ──"
	@echo "  make up-all           - Infra + observability + all services"
	@echo "  make down-all         - Stop everything"
	@echo "  make test             - Run all tests"
	@echo "  make clean            - Clean build + remove infra volumes"

# ── Infrastructure ──────────────────────────────────────────────────
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

health:
	@./scripts/health-check.sh

create-topics:
	@./infra/kafka/create-topics.sh

# ── Build ───────────────────────────────────────────────────────────
build:
	mvn clean install -DskipTests

test:
	mvn verify

clean:
	mvn clean
	docker-compose down -v
	docker-compose -f docker-compose.services.yml down -v 2>/dev/null || true
	docker-compose -f docker-compose.observability.yml down -v 2>/dev/null || true

# ── Application services ────────────────────────────────────────────
build-services:
	mvn clean package -DskipTests
	docker-compose -f docker-compose.services.yml build

up-services:
	docker-compose -f docker-compose.services.yml up -d
	@echo "All 18 services starting. Tail logs with: make logs-services"

down-services:
	docker-compose -f docker-compose.services.yml down

logs-services:
	docker-compose -f docker-compose.services.yml logs -f

rebuild-service:
	@if [ -z "$(SVC)" ]; then echo "Usage: make rebuild-service SVC=<service-name>"; exit 1; fi
	mvn -pl services/$(SVC) -am package -DskipTests
	docker-compose -f docker-compose.services.yml up -d --build --no-deps $(SVC)

health-services:
	@./scripts/health-services.sh

# ── Observability ───────────────────────────────────────────────────
up-observability:
	docker-compose -f docker-compose.observability.yml up -d

down-observability:
	docker-compose -f docker-compose.observability.yml down

# ── All-in-one ──────────────────────────────────────────────────────
up-all: up build-services up-services up-observability
	@echo ""
	@echo "Everything is up. Wait ~60s for services to register, then:"
	@echo "  API Gateway:  http://localhost:8080"
	@echo "  Grafana:      http://localhost:3000  (admin/admin)"
	@echo "  Prometheus:   http://localhost:9090"
	@echo "  Kafka UI:     http://localhost:8091"
	@echo "  RabbitMQ UI:  http://localhost:15672 (smartcommerce/smartcommerce)"
	@echo "  pgAdmin:      http://localhost:5050  (admin@smartcommerce.local/admin)"

down-all:
	-docker-compose -f docker-compose.services.yml down
	-docker-compose -f docker-compose.observability.yml down
	-docker-compose down
