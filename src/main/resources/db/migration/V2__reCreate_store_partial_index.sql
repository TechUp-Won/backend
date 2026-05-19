ALTER TABLE stores DROP CONSTRAINT uk2yl4gisselw9ehmnupuqhl3ao;

CREATE UNIQUE INDEX uk_store_seller_active
    ON stores (seller_id)
    WHERE deleted_at IS NULL;