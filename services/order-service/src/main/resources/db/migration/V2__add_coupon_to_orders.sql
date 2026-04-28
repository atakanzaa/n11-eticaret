ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50);
ALTER TABLE orders ADD COLUMN coupon_discount DECIMAL(15,2);

CREATE INDEX idx_orders_coupon_code ON orders(coupon_code) WHERE coupon_code IS NOT NULL;
