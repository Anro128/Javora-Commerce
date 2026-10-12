CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       REFERENCES categories (id),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    name        VARCHAR(200)   NOT NULL,
    slug        VARCHAR(220)   NOT NULL,
    description TEXT,
    base_price  NUMERIC(15, 2) NOT NULL,
    weight_gram INTEGER        NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT ck_products_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_products_weight CHECK (weight_gram > 0)
);

CREATE INDEX idx_products_category_status ON products (category_id, status);
CREATE INDEX idx_products_status_created_at ON products (status, created_at DESC);

CREATE TABLE product_variants (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT         NOT NULL REFERENCES products (id),
    sku        VARCHAR(64)    NOT NULL,
    name       VARCHAR(100)   NOT NULL,
    price      NUMERIC(15, 2) NOT NULL,
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ    NOT NULL,
    updated_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_product_variants_sku UNIQUE (sku),
    CONSTRAINT ck_product_variants_price CHECK (price >= 0)
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);

CREATE TABLE product_images (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products (id),
    object_key VARCHAR(255) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id, sort_order);

CREATE TABLE inventory (
    variant_id        BIGINT      PRIMARY KEY REFERENCES product_variants (id),
    quantity          INTEGER     NOT NULL,
    reserved_quantity INTEGER     NOT NULL DEFAULT 0,
    version           BIGINT      NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_inventory_reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity)
);
