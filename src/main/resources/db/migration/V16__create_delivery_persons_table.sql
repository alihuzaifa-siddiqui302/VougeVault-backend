CREATE TABLE delivery_persons (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(15),
    vehicle_type VARCHAR(50),
    is_available BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_persons_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_persons_available ON delivery_persons(is_available);