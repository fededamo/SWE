CREATE INDEX IF NOT EXISTS idx_users_role_active ON users (role, active);
CREATE INDEX IF NOT EXISTS idx_users_manager ON users (manager_id);

CREATE INDEX IF NOT EXISTS idx_vehicle_models_brand ON vehicle_models (brand_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_catalog ON vehicles (purpose, status, model_id);
CREATE INDEX IF NOT EXISTS idx_discounts_active_period ON discounts (enabled, starts_on, ends_on);

CREATE INDEX IF NOT EXISTS idx_rentals_customer_status ON rentals (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_rentals_salesman_status ON rentals (salesman_id, status);
CREATE INDEX IF NOT EXISTS idx_rentals_vehicle_period ON rentals (vehicle_id, starts_on, ends_on, status);

CREATE INDEX IF NOT EXISTS idx_test_drives_customer_status ON test_drives (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_test_drives_salesman_status ON test_drives (salesman_id, status);
CREATE INDEX IF NOT EXISTS idx_test_drives_vehicle_schedule ON test_drives (vehicle_id, scheduled_at, ends_at, status);

CREATE INDEX IF NOT EXISTS idx_sale_orders_customer_status ON sale_orders (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_sale_orders_salesman_status ON sale_orders (salesman_id, status);
CREATE INDEX IF NOT EXISTS idx_sale_orders_vehicle_status ON sale_orders (vehicle_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_rental ON payments (rental_id);
CREATE INDEX IF NOT EXISTS idx_payments_sale_order ON payments (sale_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_reference_lookup ON payments (rental_id, sale_order_id, created_at);

CREATE INDEX IF NOT EXISTS idx_purchase_proposals_customer ON purchase_proposals (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_purchase_proposals_salesman ON purchase_proposals (salesman_id, status);
CREATE INDEX IF NOT EXISTS idx_purchase_proposals_manager ON purchase_proposals (manager_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_orders_manager_status ON stock_orders (manager_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_orders_model ON stock_orders (model_id);
