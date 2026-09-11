-- Legacy decisions retain NULL: the historic decision time cannot be reconstructed.
ALTER TABLE purchase_proposals ADD COLUMN decided_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_proposals ADD CONSTRAINT ck_purchase_proposals_decision_time CHECK (
    decided_at IS NULL
    OR (status IN ('APPROVED', 'REJECTED') AND decided_at >= requested_at)
);

-- SQL CHECK accepts UNKNOWN: the original price check accidentally allowed NULL.
ALTER TABLE vehicles DROP CONSTRAINT ck_vehicles_required_price;
ALTER TABLE vehicles ADD CONSTRAINT ck_vehicles_required_price CHECK (
    (purpose = 'FOR_SALE' AND sale_price IS NOT NULL AND sale_price > 0)
    OR (purpose = 'RENTAL' AND daily_rental_rate IS NOT NULL AND daily_rental_rate > 0)
    OR purpose = 'ACQUISITION_REQUEST'
);
