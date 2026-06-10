-- ==========================================
--  Auth 도메인
-- ==========================================
CREATE TABLE auths (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE auth_locals (
    id BIGSERIAL PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_updated_at TIMESTAMP,
    failed_attempts_count BIGINT DEFAULT 0,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_auth_locals_auth FOREIGN KEY (auth_id) REFERENCES auths(id)
);
CREATE UNIQUE INDEX uk_auth_locals_email_active ON auth_locals (email) WHERE deleted_at IS NULL;

CREATE TABLE auth_socials (
    id BIGSERIAL PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    provider_refresh_token VARCHAR(500),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_auth_socials_auth FOREIGN KEY (auth_id) REFERENCES auths(id)
);
CREATE UNIQUE INDEX uk_auth_socials_provider_active ON auth_socials (provider, provider_id) WHERE deleted_at IS NULL;

CREATE TABLE login_histories (
    id BIGSERIAL PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(50) NOT NULL,
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_histories_auth FOREIGN KEY (auth_id) REFERENCES auths(id)
);

CREATE TABLE oauth_revocation_failures (
   id BIGSERIAL PRIMARY KEY,
   provider VARCHAR(50) NOT NULL,
   provider_id VARCHAR(255) NOT NULL,
   provider_refresh_token VARCHAR(500),
   status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
   retry_count BIGINT NOT NULL DEFAULT 0,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP
);

CREATE INDEX idx_auth_locals_email ON auth_locals(email);
CREATE INDEX idx_auth_socials_provider_id ON auth_socials(provider, provider_id);

-- ==========================================
--  User & Friend 도메인
-- ==========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(13),
    image VARCHAR(255) DEFAULT 'http://default.png',
    gender VARCHAR(20) DEFAULT 'NONE',
    birth_date DATE,
    marketing_agree BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_users_auth FOREIGN KEY (auth_id) REFERENCES auths(id)
);
CREATE UNIQUE INDEX uk_users_phone_active ON users (phone) WHERE deleted_at IS NULL;

CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    alias VARCHAR(20),
    memo TEXT,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_friends_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_friends_target FOREIGN KEY (target_id) REFERENCES users(id)
);
CREATE UNIQUE INDEX uk_friends_user_friend ON friends (user_id, target_id);

-- ==========================================
--  Seller & Store 도메인
-- ==========================================
CREATE TABLE sellers (
                         id BIGSERIAL PRIMARY KEY,
                         auth_id BIGINT NOT NULL,
                         buz_no VARCHAR(10) NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         phone VARCHAR(20) NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP,
                         deleted_at TIMESTAMP,
                         CONSTRAINT fk_sellers_auth FOREIGN KEY (auth_id) REFERENCES auths(id)
);
CREATE UNIQUE INDEX uk_sellers_buz_no_active ON sellers (buz_no) WHERE deleted_at IS NULL;

CREATE TABLE stores (
                        id BIGSERIAL PRIMARY KEY,
                        seller_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        phone VARCHAR(20) NOT NULL,
                        thumbnail VARCHAR(255) DEFAULT 'http://default.png',
                        status VARCHAR(50) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP,
                        deleted_at TIMESTAMP,
                        CONSTRAINT fk_stores_seller FOREIGN KEY (seller_id) REFERENCES sellers(id)
);
CREATE UNIQUE INDEX uk_stores_seller_active ON stores (seller_id) WHERE deleted_at IS NULL;

-- ==========================================
--  Term 도메인 & Consents
-- ==========================================
CREATE TABLE terms (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    is_required BOOLEAN NOT NULL
);

CREATE TABLE term_versions (
    id BIGSERIAL PRIMARY KEY,
    term_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_term_versions_term FOREIGN KEY (term_id) REFERENCES terms(id)
);

CREATE TABLE user_consents (
    id BIGSERIAL PRIMARY KEY,
    terms_version_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_consents_term_version FOREIGN KEY (terms_version_id) REFERENCES term_versions(id),
    CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE seller_consents (
    id BIGSERIAL PRIMARY KEY,
    terms_version_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_consents_term_version FOREIGN KEY (terms_version_id) REFERENCES term_versions(id),
    CONSTRAINT fk_seller_consents_seller FOREIGN KEY (seller_id) REFERENCES sellers(id)
);

-- ==========================================
--  Product 도메인
-- ==========================================
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    depth INT NOT NULL,
    parent_category_id BIGINT,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories(id)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    thumbnail VARCHAR(255),
    price INT NOT NULL,
    discount_rate INT,
    discounted_price INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_details (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_details_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE deleted_product_images (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL,
    deleted_at TIMESTAMP NOT NULL
);

CREATE TABLE product_option_groups (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_option_groups_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE product_options (
    id BIGSERIAL PRIMARY KEY,
    product_option_group_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_options_group FOREIGN KEY (product_option_group_id) REFERENCES product_option_groups(id)
);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    stock INT NOT NULL,
    name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE variant_option_maps (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    CONSTRAINT fk_vom_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_vom_option FOREIGN KEY (product_option_id) REFERENCES product_options(id),
    CONSTRAINT uk_variant_option UNIQUE (variant_id, product_option_id)
);

CREATE TABLE stock_histories (
    id BIGSERIAL PRIMARY KEY,
    product_variant_id BIGINT NOT NULL,
    order_id BIGINT,
    change_amount INT NOT NULL,
    stock_before INT NOT NULL,
    stock_after INT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_history_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

-- ==========================================
--  Shipping 도메인
-- ==========================================
CREATE TABLE shipping_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    address1 VARCHAR(255) NOT NULL,
    address2 VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    memo VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_shipping_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ==========================================
--  Order & Payment 도메인
-- ==========================================

CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    original_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    point_used_amount BIGINT NOT NULL DEFAULT 0,
    final_amount BIGINT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX uk_orders_order_number ON orders (order_number) WHERE deleted_at IS NULL;

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    option_summary VARCHAR(255),
    product_amount BIGINT NOT NULL,
    quantity INT NOT NULL,
    product_image_url TEXT,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    zipcode VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    address_detail VARCHAR(255),
    memo VARCHAR(255),
    delivery_status VARCHAR(50) NOT NULL,
    delivery_company VARCHAR(100),
    tracking_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE payments (
    payment_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    pg_provider VARCHAR(50) NOT NULL,
    toss_order_id VARCHAR(100) NOT NULL,
    payment_key VARCHAR(100),
    idempotency_key VARCHAR(100) NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    fail_code VARCHAR(100),
    fail_message TEXT,
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE UNIQUE INDEX uk_payments_toss_order_id ON payments (toss_order_id);
CREATE UNIQUE INDEX uk_payments_idempotency_key ON payments (idempotency_key);

-- ==========================================
--  Chat 도메인
-- ==========================================

-- 1. 채팅방 테이블
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    room_type VARCHAR(20) NOT NULL,
    title VARCHAR(255),
    room_image TEXT,
    participant_count INT NOT NULL DEFAULT 0,
    last_message_content TEXT,
    last_message_at TIMESTAMP,
    room_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- 2. 메시지 테이블 (chat_rooms 참조)
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT,
    answer_message_id BIGINT,
    message_type VARCHAR(20) NOT NULL,
    content TEXT,
    like_count INT NOT NULL DEFAULT 0,
    version BIGINT,
    message_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,

    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
    FOREIGN KEY (answer_message_id) REFERENCES chat_messages(id)
);

-- 3. 채팅방 참여자 테이블
CREATE TABLE chat_participants (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    room_title VARCHAR(255),
    room_image TEXT,
    last_read_message_id BIGINT,
    is_alarm_on BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
    FOREIGN KEY (last_read_message_id) REFERENCES chat_messages(id)
);

-- 4. 메시지 숨김 테이블
CREATE TABLE message_hides (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    CONSTRAINT uk_message_hide UNIQUE (message_id, user_id)
);

-- 5. 메시지 좋아요 테이블
CREATE TABLE message_likes (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    like_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    canceled_at TIMESTAMP,

    FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    CONSTRAINT uk_message_like UNIQUE (message_id, user_id)
);

-- ==========================================
-- 7.6. 성능 최적화를 위한 핵심 인덱스 선언  << AI가 써준 부분인데 필요하시면 아래의 주석을 새로운 스크립트로 작성해서 사용하세요!
-- ==========================================

-- 특정 채팅방의 메시지 이력을 최신순으로 페이지네이션 조회할 때의 성능 향상
-- CREATE INDEX idx_chat_messages_room_history ON chat_messages (chat_room_id, created_at DESC);

-- 특정 사용자가 참여 중인 채팅방 목록을 빠르게 인덱싱하여 반정규화 조회 성능 방어
-- CREATE INDEX idx_chat_participants_user ON chat_participants (user_id);