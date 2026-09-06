CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text TEXT,
    verified_purchase BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id)
);

CREATE INDEX idx_reviews_customer_id ON reviews(customer_id);
CREATE INDEX idx_reviews_order_item_id ON reviews(order_item_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- Index for finding reviews by product (via order_item → product_variant → product)
CREATE INDEX idx_reviews_product_id ON reviews(created_at DESC);