-- ==========================================
--  Product Like 도메인
-- ==========================================
CREATE TABLE product_likes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_likes_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_product_likes_product_user UNIQUE (product_id, user_id)
);
