# ADR-009: Payment Provider — Iyzico (Real Integration)

**Status:** Accepted

## Context
Need a Turkish-market payment provider integration for the showcase. Stripe is excluded
(no Turkish bank rails); PayTR/Param require manual onboarding. Iyzico has a sandbox,
official Java SDK (`com.iyzipay:iyzipay-java`), and supports the 3DS flow that Turkish
banks mandate.

## Decision
- Use Iyzico via `iyzipay-java` 2.0.61.
- Sandbox base URL: `https://sandbox-api.iyzipay.com`.
- Hide the SDK behind a `PaymentProvider` interface (records for input/output) so the
  service code never touches Iyzico types directly. Only `IyzicoPaymentAdapter`
  imports `com.iyzipay.*`. This makes the provider swappable and keeps unit tests
  free of the SDK.
- Always use the 3DS flow (`ThreedsInitialize.create` → callback → `ThreedsPayment.create`).
  Even for low-risk amounts; matches Turkish bank expectations.
- Persist every Iyzico exchange in `payment_attempts` (request and response JSONB) for
  audit and debugging. Card PAN/CVC are redacted before persisting.
- Persist every webhook hit in `webhook_events` (idempotency + audit) before processing.
- Hourly `PaymentReconciliationJob` cross-checks our payment state against Iyzico
  (`Payment.retrieve`) for payments that didn't reach a terminal state, fixing status
  drift caused by missed/late callbacks.

## State Machine
```
INITIATED → THREEDS_PENDING → THREEDS_AUTHENTICATED → CAPTURING → SUCCEEDED
                                       ↓                  ↓           ↓
                                       FAILED            FAILED   PARTIALLY_REFUNDED → REFUNDED
```

## Event Contract (outbox → Kafka)
- `payment.initiated.v1` — first attempt persisted, before 3DS HTML returned to client
- `payment.succeeded.v1` — capture success or reconciliation catch-up
- `payment.failed.v1` — initialize / 3DS / capture failure
- `payment.refunded.v1` — full or partial refund completed

`order-service` already consumes the success/failure topics (Day 3) and confirms or
compensates the order accordingly.

## Trade-offs
- The Iyzico 3DS callback is browser-driven (POST from user's browser, not S2S), so
  signature validation does not apply. We rely on `conversationId` lookup +
  `processed_events` idempotency. The reconciliation job is the safety net.
- We store BigDecimals as DECIMAL(15,2). The SDK uses BigDecimal natively in 2.0.x.
- For v1 we only support full refunds via API; partial refunds are supported by the
  state machine but not exposed.
- Iyzico requires an 11-digit T.C. identity number on the buyer object. We pass a
  sandbox placeholder (`11111111111`) — production needs a KVKK-aware capture flow.
