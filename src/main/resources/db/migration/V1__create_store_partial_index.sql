CREATE TABLE stores (
                        id BIGSERIAL PRIMARY KEY,
                        seller_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        phone VARCHAR(255) NOT NULL,
                        thumbnail VARCHAR(255) NOT NULL,
                        status VARCHAR(255) NOT NULL,
                        deleted_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP
);

ALTER TABLE stores DROP CONSTRAINT IF EXISTS uk2yl4gisselw9ehmnupuqhl3ao;

CREATE UNIQUE INDEX IF NOT EXISTS uk_store_seller_active
ON stores (seller_id)
WHERE deleted_at IS NULL;