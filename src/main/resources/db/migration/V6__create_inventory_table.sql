CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_variant_id BIGINT NOT NULL UNIQUE,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_variant FOREIGN KEY (product_variant_id) REFERENCES products_variants(id),
    CONSTRAINT chk_stock_non_negative CHECK (stock_quantity >= 0)
);
