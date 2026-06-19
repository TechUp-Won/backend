CREATE INDEX idx_product_store_popular ON products (store_id, like_count DESC, id DESC);
CREATE INDEX idx_product_store_latest ON products (store_id, created_at DESC, id DESC);
