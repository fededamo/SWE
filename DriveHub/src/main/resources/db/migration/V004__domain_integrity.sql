-- Keep historical data intact: invalid existing rows stop migration instead of being rewritten.
ALTER TABLE vehicles ADD CONSTRAINT ck_vehicles_inapplicable_prices CHECK (
    (purpose = 'FOR_SALE' AND daily_rental_rate IS NULL)
    OR (purpose = 'RENTAL' AND sale_price IS NULL)
    OR (purpose = 'ACQUISITION_REQUEST' AND sale_price IS NULL AND daily_rental_rate IS NULL)
);
ALTER TABLE test_drives ADD CONSTRAINT ck_test_drives_min_duration CHECK (
    ends_at >= scheduled_at + INTERVAL '15' MINUTE
);
ALTER TABLE discounts ADD CONSTRAINT ck_discounts_positive_total CHECK (percentage < 100);
ALTER TABLE sale_orders ADD CONSTRAINT ck_sale_orders_payment_state CHECK (
    (status = 'RESERVED' AND paid_amount < required_deposit)
    OR (status = 'DEPOSIT_PAID' AND paid_amount >= required_deposit AND paid_amount < total_price)
    OR (status = 'PAID' AND paid_amount = total_price)
    OR (status = 'COMPLETED' AND paid_amount = total_price AND salesman_id IS NOT NULL)
    OR (status = 'CANCELLED' AND paid_amount = 0)
);
