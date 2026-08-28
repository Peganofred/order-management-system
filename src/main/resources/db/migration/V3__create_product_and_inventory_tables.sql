-- V3__create_product_and_inventory_tables.sql
-- Phase 3: product catalog + inventory.

CREATE TABLE categories (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

-- Seed data: lets an admin create/test products immediately (via
-- GET /api/v1/categories to discover valid IDs) without needing a
-- category-management API, which is deliberately out of scope for Phase 3.
INSERT INTO categories (name, slug) VALUES
    ('Electronics', 'electronics'),
    ('Books', 'books'),
    ('Clothing', 'clothing'),
    ('Home & Kitchen', 'home-kitchen');

CREATE TABLE products (
    id            UUID          PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    description   TEXT,
    price         NUMERIC(12,2) NOT NULL,
    sku           VARCHAR(64)   NOT NULL,
    category_id   BIGINT        NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price_non_negative CHECK (price >= 0)
);

-- Same reasoning as V2 for users.id: JVM-generated UUID (@UuidGenerator),
-- not a Postgres default, so ID generation stays visible in Java code.

-- Indexes for the filters ProductSpecification actually builds
-- (category, price range, name search). Without these, every catalog
-- listing query is a sequential scan once the products table grows.
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_active ON products (active);

CREATE TABLE inventory (
    product_id      UUID      PRIMARY KEY,
    stock_quantity  INTEGER   NOT NULL DEFAULT 0,
    version         BIGINT    NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    -- Last line of defense against overselling: even if a future
    -- decrement bug in application code (bad locking, a missed check)
    -- tries to push stock below zero, the database itself refuses the
    -- write. See Inventory.java javadoc for the full locking discussion.
    CONSTRAINT chk_inventory_stock_non_negative CHECK (stock_quantity >= 0)
);

-- inventory.product_id is BOTH the primary key and the foreign key to
-- products - this is the "shared primary key" 1:1 pattern (Hibernate's
-- @MapsId on the Inventory entity). It guarantees at most one inventory
-- row per product at the schema level, with no extra unique constraint
-- and no separate surrogate id to keep in sync.
COMMENT ON TABLE inventory IS 'One row per product, PK = product_id (shared PK 1:1 with products). See com.portfolio.orderms.entity.Inventory';
