CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL,
    max_discount_amount DECIMAL(15,2),
    minimum_order_amount DECIMAL(15,2),
    applicable_category_ids JSONB DEFAULT '[]'::jsonb,
    applicable_seller_ids JSONB DEFAULT '[]'::jsonb,
    excluded_product_ids JSONB DEFAULT '[]'::jsonb,
    total_usage_limit INT,
    per_user_limit INT NOT NULL DEFAULT 1,
    times_used INT NOT NULL DEFAULT 0,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_first_order_only BOOLEAN NOT NULL DEFAULT FALSE,
    is_stackable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_active ON coupons(is_active, valid_from, valid_until);

CREATE TABLE coupon_usages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    coupon_id UUID NOT NULL REFERENCES coupons(id),
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (coupon_id, order_id)
);

CREATE INDEX idx_coupon_usages_user ON coupon_usages(user_id);
CREATE INDEX idx_coupon_usages_coupon ON coupon_usages(coupon_id);

INSERT INTO coupons (code, name, description, discount_type, discount_value, minimum_order_amount, valid_from, valid_until) VALUES
    ('WELCOME10', 'Hoş Geldin İndirimi', 'Yeni kullanıcılara %10 indirim', 'PERCENTAGE', 10.00, 100.00, NOW(), NOW() + INTERVAL '90 days'),
    ('FREESHIP', 'Ücretsiz Kargo', '50 TL üzeri siparişlerde kargo bedava', 'FREE_SHIPPING', 0, 50.00, NOW(), NOW() + INTERVAL '90 days'),
    ('BIGSAVE100', '100 TL İndirim', '500 TL üzeri siparişlerde 100 TL indirim', 'FIXED_AMOUNT', 100.00, 500.00, NOW(), NOW() + INTERVAL '30 days');

CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_promotion_outbox_pending ON outbox(status, created_at) WHERE status = 'PENDING';

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
