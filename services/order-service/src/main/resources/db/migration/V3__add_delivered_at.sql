-- Customer-driven delivery confirmation. Set when the customer presses
-- "Siparişi Teslim Aldım"; nullable so historical orders are unaffected.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ;
