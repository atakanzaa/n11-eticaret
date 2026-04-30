# ADR-012: Observability Stack — Prometheus + Grafana + Loki + Tempo

**Status:** Accepted

## Context
A microservice platform without unified metrics/logs/traces is impossible to
operate or to demo convincingly in a job interview. We need: per-service
metrics, business KPIs, correlatable structured logs, and end-to-end traces.

## Decision
**Stack (`docker-compose.observability.yml`):**
- **Prometheus** (`:9090`) scrapes `/actuator/prometheus` from every service via
  `host.docker.internal` (services run on host, observability runs in Docker).
- **Grafana** (`:3000`, admin/admin) auto-provisions Prometheus + Loki + Tempo
  datasources and two pre-built dashboards from
  `infra/observability/grafana/dashboards/`.
- **Loki** (`:3100`) — log aggregation backend. Day 5 sets up the JSON encoder
  in `shared/common-web/src/main/resources/logback-spring.xml` so every
  service emits structured JSON with `correlationId`, `traceId`, `spanId`,
  `service`. Log shipping to Loki happens via Promtail or Docker logging
  driver in Day 6 once services are dockerized.
- **Tempo** (`:3200`, OTLP `:4317/:4318`) — trace storage. OpenTelemetry
  auto-instrumentation gets wired in Day 6 alongside Docker images.

**Custom business metrics (Day 5):**
- `smartcommerce_orders_created_total` (CheckoutOrchestrator) — counter
- `smartcommerce_orders_confirmed_total` (CheckoutOrchestrator) — counter
- `smartcommerce_saga_compensations_total` — counter, increments on rollback
- `smartcommerce_checkout_duration_seconds` — Timer
- `smartcommerce_payment_success_total` / `_failure_total` / `_refund_total`
  (PaymentService) — counters

**Dashboards:**
- `smartcommerce-overview.json` — service up/down, request rate, p95 latency,
  5xx rate, JVM heap, DB pool usage.
- `smartcommerce-business.json` — orders created, saga compensations,
  checkout duration p95, payment success/failure rate.

## Trade-offs
- Loki + Tempo without an OTel agent in Day 5 means traceId/spanId fields
  in log JSON will be empty until Day 6 wires the OTel SDK. The JSON
  schema is forward-compatible — once OTel publishes to MDC, the same logs
  will carry trace context with no service code change.
- We chose Tempo (Grafana stack) over Jaeger because we already use Loki and
  Grafana, and Tempo's tracesToLogs correlation works zero-config.
- `host.docker.internal` is Docker Desktop-only (Mac/Windows). The
  Day 6 dockerization will switch services into the same network and use
  service names instead.
