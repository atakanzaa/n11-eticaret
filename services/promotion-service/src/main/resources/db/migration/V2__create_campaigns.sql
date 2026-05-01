CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    offer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    product_id UUID NOT NULL,
    discounted_price DECIMAL(12,2) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_campaign_dates CHECK (ends_at > starts_at)
);

CREATE INDEX idx_campaigns_offer ON campaigns(offer_id);
CREATE INDEX idx_campaigns_seller ON campaigns(seller_id);
CREATE INDEX idx_campaigns_active_window ON campaigns(is_active, starts_at, ends_at) WHERE is_active = TRUE;
