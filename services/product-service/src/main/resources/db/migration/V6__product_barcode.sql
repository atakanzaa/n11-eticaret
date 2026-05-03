-- Optional global product identifier. NULLs allowed for legacy rows; partial
-- unique index ensures duplicates can never be inserted with the same barcode.
ALTER TABLE products ADD COLUMN IF NOT EXISTS barcode VARCHAR(32);
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode
    ON products(barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;
