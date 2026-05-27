DO $$
BEGIN
    IF to_regclass('public.order_items') IS NOT NULL THEN
        ALTER TABLE order_items
            ADD COLUMN IF NOT EXISTS product_image_url TEXT;
    END IF;
END $$;
