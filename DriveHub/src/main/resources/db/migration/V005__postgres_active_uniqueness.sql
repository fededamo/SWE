-- PostgreSQL-only partial indexes. H2 tests do not verify these guarantees.
ALTER TABLE test_drives DROP CONSTRAINT uq_test_drives_vehicle_slot;
ALTER TABLE test_drives DROP CONSTRAINT uq_test_drives_customer_slot;
CREATE UNIQUE INDEX uq_test_drives_live_vehicle_slot ON test_drives(vehicle_id, scheduled_at)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED');
CREATE UNIQUE INDEX uq_test_drives_live_customer_slot ON test_drives(customer_id, scheduled_at)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED');
CREATE UNIQUE INDEX uq_sale_orders_live_vehicle ON sale_orders(vehicle_id)
    WHERE status <> 'CANCELLED';
CREATE UNIQUE INDEX uq_payments_completed_rental ON payments(rental_id)
    WHERE status = 'COMPLETED' AND rental_id IS NOT NULL;
