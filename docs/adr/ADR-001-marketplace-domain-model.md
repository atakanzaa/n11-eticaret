# ADR-001: Marketplace Domain Model

**Status:** Accepted

## Context
Need to model the relationship between products and seller listings.

## Decision
- **Product** is master data (title, brand, category, attributes)
- **Offer** is a seller's listing of a product (price, stock, cargo)
- One Product can have many Offers (5 sellers selling same iPhone)
- Offer is an aggregate INSIDE product-service (not a separate service)

## Consequences
- Saves time without losing the abstraction
- product-service is the single source of truth for catalog data
