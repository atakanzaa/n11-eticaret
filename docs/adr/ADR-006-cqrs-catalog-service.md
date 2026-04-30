# ADR-006: CQRS via catalog-service (OpenSearch projection)

**Status:** Accepted

## Context
Search needs Turkish-aware analyzers, faceting, fuzzy matching, and price-range queries.
JPA queries against `product_db` cannot satisfy these efficiently and would couple search
load to the write model. The platform already has Kafka for domain events, so an
event-driven projection is cheap.

## Decision
- **product-service** is the write model (Product + Offer aggregate, master data).
- **catalog-service** is the read model: an OpenSearch index (`products`) built from
  Kafka events, exposed via `/api/search` and `/api/catalog/products/{id}`.
- Index schema includes Turkish analyzer (`turkish_analyzer` = standard tokenizer +
  lowercase + turkish_stop + turkish_stemmer + asciifolding) for `title`, `description`,
  `categoryName`. Numeric fields use `scaled_float` for price (avoids floating-point
  drift on aggregations).
- Projection consumer subscribes to:
  `product.created.v1`, `product.updated.v1`, `product.deleted.v1`,
  `offer.created.v1`, `offer.price-changed.v1`, `offer.status-changed.v1`,
  `offer.stock-changed.v1`.
- On any of these, we re-fetch the product + all its offers from product-service via
  Feign and re-index the full document. This is simpler than maintaining a per-aggregate
  state table and is acceptable for the showcase volume.
- Search supports: free-text query, category/brand filter, price range, in-stock filter,
  sort (relevance/price/rating/newest), paging, and faceting (by_category, by_brand).

## Trade-offs
- **Eventual consistency:** there is a 1-2 second window between an offer price change
  and its visibility in search. Acceptable for marketplace browsing.
- **Re-fetch on every event** is more expensive than incremental updates, but the cost
  is bounded (one product + ~5 offers per event) and the code is dramatically simpler.
- The Kafka consumer is *not* idempotent at the message level (no `processed_events`
  table). Replay just re-indexes the same document — naturally idempotent at the
  destination.
- An `/api/catalog/internal/products/{id}/reindex` endpoint exists for manual recovery
  after a backfill or incident.
