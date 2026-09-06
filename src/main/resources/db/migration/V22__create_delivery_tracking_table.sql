-- Simplified location tracking (lat/lon only)
CREATE TABLE delivery_locations (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_locations_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_locations_delivery_id ON delivery_locations(delivery_id);
CREATE INDEX idx_delivery_locations_created_at ON delivery_locations(created_at);

-- OTP verification for delivery
CREATE TABLE delivery_otps (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL UNIQUE,
    otp_code VARCHAR(6) NOT NULL,
    attempts_left INTEGER DEFAULT 3,
    is_verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_delivery_otps_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_otps_delivery_id ON delivery_otps(delivery_id);
CREATE INDEX idx_delivery_otps_expires_at ON delivery_otps(expires_at);

-- Delivery person statistics
CREATE TABLE delivery_stats (
    id BIGSERIAL PRIMARY KEY,
    delivery_person_id BIGINT NOT NULL UNIQUE,
    total_deliveries INTEGER DEFAULT 0,
    successful_deliveries INTEGER DEFAULT 0,
    failed_deliveries INTEGER DEFAULT 0,
    average_rating DECIMAL(3, 2),
    total_earnings DECIMAL(10, 2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_stats_delivery_person FOREIGN KEY (delivery_person_id) REFERENCES delivery_persons(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_stats_delivery_person_id ON delivery_stats(delivery_person_id);