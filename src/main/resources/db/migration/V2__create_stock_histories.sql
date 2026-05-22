CREATE TABLE stock_histories (
    id                  BIGSERIAL       PRIMARY KEY,
    product_variant_id  BIGINT          NOT NULL REFERENCES product_variants(id),
    order_id            BIGINT          REFERENCES orders(id),
    change_amount       INT             NOT NULL,
    stock_before        INT             NOT NULL,
    stock_after         INT             NOT NULL,
    reason              VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);
