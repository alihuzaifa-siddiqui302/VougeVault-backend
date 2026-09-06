CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,                    -- Unique ID for each image
    product_id BIGINT NOT NULL,                  -- Links to which product
    file_name VARCHAR(255) NOT NULL,             -- Actual filename (UUID.jpg)
    file_path VARCHAR(500) NOT NULL,             -- Where it's stored
    original_file_name VARCHAR(255),             -- What user named it (photo.jpg)
    file_size BIGINT,                            -- Size in bytes
    mime_type VARCHAR(100),                      -- Type: image/jpeg, image/png
    is_primary BOOLEAN DEFAULT false,            -- Is this the main image?
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);