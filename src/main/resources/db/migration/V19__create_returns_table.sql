CREATE TABLE returns (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(500),
    refund_amount NUMERIC(10, 2),
    stripe_refund_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_returns_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_returns_customer FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- Junction table: Return can have multiple OrderItems
CREATE TABLE return_items (
    id BIGSERIAL PRIMARY KEY,
    return_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity_returned INTEGER NOT NULL,
    refund_per_unit NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_return_items_return FOREIGN KEY (return_id) REFERENCES returns(id),
    CONSTRAINT fk_return_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);

CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_customer_id ON returns(customer_id);
CREATE INDEX idx_returns_status ON returns(status);
CREATE INDEX idx_return_items_return_id ON return_items(return_id);