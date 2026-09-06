CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    delivery_person_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    pickup_address VARCHAR(500) NOT NULL,
    drop_address VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_deliveries_delivery_person FOREIGN KEY (delivery_person_id) REFERENCES delivery_persons(id)
);

CREATE INDEX idx_deliveries_delivery_person_id ON deliveries(delivery_person_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);