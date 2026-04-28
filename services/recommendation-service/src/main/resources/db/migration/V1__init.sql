CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE user_behaviors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    metadata JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_behaviors_user ON user_behaviors(user_id, occurred_at DESC);
CREATE INDEX idx_behaviors_product ON user_behaviors(product_id, occurred_at DESC);
CREATE INDEX idx_behaviors_action ON user_behaviors(action_type, occurred_at DESC);

CREATE TABLE product_popularity (
    product_id UUID PRIMARY KEY,
    view_count BIGINT NOT NULL DEFAULT 0,
    cart_add_count BIGINT NOT NULL DEFAULT 0,
    purchase_count BIGINT NOT NULL DEFAULT 0,
    score DECIMAL(15,4) NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_popularity_score ON product_popularity(score DESC);

CREATE TABLE co_purchase_pairs (
    product_a_id UUID NOT NULL,
    product_b_id UUID NOT NULL,
    co_purchase_count INT NOT NULL DEFAULT 1,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_a_id, product_b_id)
);

CREATE INDEX idx_copurchase_a ON co_purchase_pairs(product_a_id, co_purchase_count DESC);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
