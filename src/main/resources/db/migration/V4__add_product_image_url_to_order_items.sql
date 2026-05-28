ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS product_image_url TEXT;
