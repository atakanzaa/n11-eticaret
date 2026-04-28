CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE fraud_checks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    risk_score INT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    triggered_rules JSONB DEFAULT '[]'::jsonb,
    user_velocity_count INT,
    user_amount_24h DECIMAL(15,2),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_checks_user ON fraud_checks(user_id, created_at DESC);
CREATE INDEX idx_fraud_checks_order ON fraud_checks(order_id);
CREATE INDEX idx_fraud_checks_decision ON fraud_checks(decision);

CREATE TABLE fraud_blacklist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blacklist_type VARCHAR(20) NOT NULL,
    value VARCHAR(255) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (blacklist_type, value)
);

CREATE INDEX idx_blacklist_lookup ON fraud_blacklist(blacklist_type, value);
