CREATE TABLE IF NOT EXISTS deleted_product_images (
    id         BIGSERIAL    PRIMARY KEY,
    url        TEXT         NOT NULL,
    deleted_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
