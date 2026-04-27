# SmartCommerce Daily Log

## Day 1 — 2026-04-27

### Completed
- Monorepo + Maven multi-module setup (Java 21, Spring Boot 3.3.4)
- Docker Compose: Postgres 16, Redis 7, Kafka (Confluent 7.5), RabbitMQ 3.13, OpenSearch 2.15, pgAdmin, Kafka-UI
- Shared modules:
  - common-events: BaseEvent, EventType, Topics, UserRegisteredPayload, SellerRegisteredPayload
  - common-errors: ErrorCode, BusinessException, ResourceNotFoundException, DuplicateResourceException, ValidationException, ErrorResponse
  - common-web: CorrelationIdFilter, GlobalExceptionHandler, ApiResponse
  - common-security: JwtService (jjwt 0.12.6), JwtAuthFilter, SecurityConfig (BCrypt strength 12)
  - common-test: AbstractIntegrationTest (Testcontainers: Postgres, Kafka, Redis)
- auth-service (port 8081):
  - Register, Login, Refresh, Logout, /me endpoints
  - JWT: access token 15min, refresh token 7 days, single-use with family rotation
  - Brute force: 5 failed attempts → 15 min lock
  - Outbox pattern: PENDING → PUBLISHED/FAILED, 1s scheduler, SHA-256 token hash
  - Flyway migration: users, roles, user_roles, refresh_tokens, outbox tables
  - Integration tests with Testcontainers
- api-gateway (port 8080): JWT validation filter, X-User-Id/X-User-Roles header injection, correlation ID propagation, CORS
- seller-service (port 8083): USER_REGISTERED consumer → auto-create seller record, idempotency via processed_events, Outbox pattern, SELLER_REGISTERED event, CRUD endpoints

### Architecture Decisions Applied
- ADR-005 (Outbox): Every Kafka publish via outbox table, 1s scheduler
- ADR-010 (JWT): 15min access token, 7-day refresh with family theft detection
- SHA-256 hash stored for refresh tokens (never raw)

### Issues Encountered
- None

### Tomorrow's Focus (Day 2)
- product-service (Product + Offer aggregate)
- user-service (profiles + addresses)
- cart-service (Redis-backed cart)
- catalog-service (OpenSearch read projection)
