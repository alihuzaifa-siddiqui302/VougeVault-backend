CREATE TABLE products_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    size VARCHAR(50) NOT NULL,
    color VARCHAR(50) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uq_product_size_color UNIQUE (product_id, size, color)
);

CREATE INDEX idx_variants_product_id ON products_variants(product_id);
