-- =====================================================================
-- V5: Review System Expansion
-- Adds: moderation, soft-delete, images, seller replies, votes, reports
-- =====================================================================

-- 1. Extend reviews table
ALTER TABLE reviews
    ADD COLUMN deleted_at         TIMESTAMP WITH TIME ZONE,
    ADD COLUMN updated_at         TIMESTAMP WITH TIME ZONE,
    ADD COLUMN moderated_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN moderated_by       UUID,
    ADD COLUMN rejection_reason   VARCHAR(500),
    ADD COLUMN unhelpful_count    INT NOT NULL DEFAULT 0,
    ADD COLUMN verified_purchase  BOOLEAN NOT NULL DEFAULT FALSE;

-- New reviews default to PENDING (moderation queue)
ALTER TABLE reviews ALTER COLUMN status SET DEFAULT 'PENDING';

CREATE INDEX idx_reviews_deleted_at      ON reviews(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_status_created  ON reviews(status, created_at) WHERE deleted_at IS NULL;

-- 2. Review images (max 5 per review, enforced in app layer)
CREATE TABLE review_images (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id     UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    image_url     VARCHAR(1000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_review_images_review_id ON review_images(review_id);

-- 3. Seller replies (one reply per review)
CREATE TABLE review_replies (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id  UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    seller_id  UUID NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (review_id)
);
CREATE INDEX idx_review_replies_seller_id ON review_replies(seller_id);

-- 4. Review votes (one vote per user per review)
CREATE TABLE review_votes (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id  UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    vote_type  VARCHAR(20) NOT NULL CHECK (vote_type IN ('HELPFUL', 'UNHELPFUL')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (review_id, user_id)
);
CREATE INDEX idx_review_votes_review_id ON review_votes(review_id);

-- 5. Review reports (one report per user per review)
CREATE TABLE review_reports (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id   UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    reason      VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (review_id, user_id)
);
CREATE INDEX idx_review_reports_status ON review_reports(status) WHERE status = 'PENDING';
