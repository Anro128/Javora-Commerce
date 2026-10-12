CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_carts_user_id UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT      NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    variant_id BIGINT      NOT NULL REFERENCES product_variants (id),
    quantity   INTEGER     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cart_items_cart_variant UNIQUE (cart_id, variant_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(30)    NOT NULL,
    user_id         BIGINT         NOT NULL REFERENCES users (id),
    status          VARCHAR(20)    NOT NULL,
    subtotal        NUMERIC(15, 2) NOT NULL,
    shipping_cost   NUMERIC(15, 2) NOT NULL,
    discount        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total           NUMERIC(15, 2) NOT NULL,
    courier         VARCHAR(30)    NOT NULL,
    courier_service VARCHAR(30)    NOT NULL,
    recipient_name  VARCHAR(100)   NOT NULL,
    phone           VARCHAR(20)    NOT NULL,
    province        VARCHAR(100)   NOT NULL,
    city            VARCHAR(100)   NOT NULL,
    district        VARCHAR(100)   NOT NULL,
    postal_code     VARCHAR(10)    NOT NULL,
    street          VARCHAR(255)   NOT NULL,
    expires_at      TIMESTAMPTZ    NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);

CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status_expires_at ON orders (status, expires_at);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id),
    variant_id   BIGINT         NOT NULL REFERENCES product_variants (id),
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    variant_name VARCHAR(100)   NOT NULL,
    sku          VARCHAR(64)    NOT NULL,
    unit_price   NUMERIC(15, 2) NOT NULL,
    quantity     INTEGER        NOT NULL,
    weight_gram  INTEGER        NOT NULL,
    line_total   NUMERIC(15, 2) NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
