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

## Day 2 — 2026-04-27

### Completed
- Added user-service (port 8082):
  - User profile entity, address entity, KVKK consent and soft delete flow
  - USER_REGISTERED Kafka consumer with processed_events idempotency
  - USER_PROFILE_UPDATED and USER_DELETED events via outbox
- Added product-service (port 8084):
  - Product CRUD, Turkish-aware slug generation, category seed migration
  - Offer aggregate with seller ownership checks, duplicate offer protection, price/status events
  - Feign client to seller-service internal by-user endpoint
- Added cart-service (port 8088):
  - Redis-backed cart cache with PostgreSQL persistence
  - Item snapshot pattern for price/title/image/seller/cargo fields
  - Cart validation detects unavailable offers and price changes
  - Cart abandonment scheduled job publishes CART_ABANDONED
- Updated api-gateway routes for user, product/offer/category, and cart endpoints
- Added seller-service internal `GET /api/sellers/by-user/{userId}` endpoint
- Added Day 2 integration test coverage for user, product/offer, and cart flows
- Expanded Postman collection with User, Product, Offer, and Cart Day 2 requests

### Verification
- `mvn -DskipTests compile` passed for the full reactor.
- `mvn -DskipTests test` passed for the full reactor.

### Notes
- Testcontainers tests are configured to skip cleanly when Docker is unavailable in the local sandbox.

## Gün 3 — 2026-04-27

### Completed
- Added inventory-service (port 8085):
  - Offer-based inventory items with optimistic locking fields
  - Reservation pattern: reserve, confirm, release, expiration job
  - OFFER_CREATED Kafka consumer creates inventory item idempotently
  - Inventory outbox events for reserved, failed, released, confirmed, and low-stock flows
- Added order-service (port 8089):
  - Checkout saga orchestrator for cart validation, address lookup, fraud stub, inventory reservation, and PAYMENT_PENDING order creation
  - Idempotency-key storage and replay/conflict handling
  - Payment success/failure Kafka consumer with inventory confirm/release compensation
  - Payment timeout job cancels pending orders and releases reservations
- Added notification-service (port 8094):
  - Kafka consumers for USER_REGISTERED, ORDER_CONFIRMED, and ORDER_CANCELLED
  - RabbitMQ email job queue + DLQ
  - notification_log persistence for sent/failed email attempts
- Updated api-gateway routes for inventory, checkout/orders, and notifications
- Added internal user/address and cart validation endpoints needed by checkout/notifications
- Expanded Postman collection with Inventory, Orders, and Notifications requests
- Added unit test coverage for inventory reservation, checkout idempotency, and notification email logging

### Verification
- `mvn -pl services/inventory-service,services/order-service,services/notification-service -am -DskipTests test` passed.
- `mvn -pl services/inventory-service,services/order-service,services/notification-service -am test` passed.

### Notes
- Notification service adds RabbitMQ AMQP dependencies, so the first Maven run may need network/cache access.
