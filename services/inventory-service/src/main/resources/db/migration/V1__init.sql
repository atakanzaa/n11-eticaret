CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    offer_id UUID NOT NULL UNIQUE,
    product_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    sold_quantity INT NOT NULL DEFAULT 0,
    incoming_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    allow_backorder BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventory_items_offer_id ON inventory_items(offer_id);
CREATE INDEX idx_inventory_items_seller_id ON inventory_items(seller_id);
CREATE INDEX idx_inventory_items_low_stock ON inventory_items(seller_id)
    WHERE available_quantity <= low_stock_threshold;

CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_key VARCHAR(255) NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    offer_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_offer_id ON reservations(offer_id);
CREATE INDEX idx_reservations_status_expires ON reservations(status, expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_reservations_key ON reservations(reservation_key);

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

CREATE INDEX idx_inventory_outbox_status_created ON outbox(status, created_at) WHERE status = 'PENDING';

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
