CREATE TABLE stores (
                        id BIGSERIAL PRIMARY KEY,
                        seller_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        deleted_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
);

ALTER TABLE stores DROP CONSTRAINT IF EXISTS uk2yl4gisselw9ehmnupuqhl3ao;

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_seller_active
ON stores (seller_id)
WHERE deleted_at IS NULL;