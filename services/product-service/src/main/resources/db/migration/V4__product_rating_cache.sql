-- Cache aggregate review stats on the products row to avoid N+1 queries on listing pages.
-- ReviewService.create() and markHelpful() recompute these values inside the same transaction
-- as the review write, so the cache is always consistent.
ALTER TABLE products
    ADD COLUMN average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;
