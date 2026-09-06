ALTER TABLE orders ADD COLUMN delivery_latitude DECIMAL(10, 8);
ALTER TABLE orders ADD COLUMN delivery_longitude DECIMAL(11, 8);

-- For now, set default coordinates (Mumbai)
UPDATE orders SET delivery_latitude = 19.0760, delivery_longitude = 72.8777
WHERE delivery_latitude IS NULL;