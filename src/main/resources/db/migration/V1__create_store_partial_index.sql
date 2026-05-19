ALTER TABLE stores DROP CONSTRAINT IF EXISTS uk2yl4gisselw9ehmnupuqhl3ao;

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_seller_active
ON stores (seller_id)
WHERE deleted_at IS NULL;