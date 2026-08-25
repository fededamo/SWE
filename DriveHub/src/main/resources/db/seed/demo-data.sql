-- Catalogo esclusivamente dimostrativo. Non contiene utenti, password o dati personali.
INSERT INTO brands (name)
SELECT 'Demo Motors'
WHERE NOT EXISTS (SELECT 1 FROM brands WHERE name = 'Demo Motors');

INSERT INTO brands (name)
SELECT 'Example Automotive'
WHERE NOT EXISTS (SELECT 1 FROM brands WHERE name = 'Example Automotive');

INSERT INTO vehicle_models (brand_id, code, name, model_year)
SELECT b.id, 'DEMO-CITY-2026', 'City Demo', 2026
FROM brands b
WHERE b.name = 'Demo Motors'
  AND NOT EXISTS (SELECT 1 FROM vehicle_models WHERE code = 'DEMO-CITY-2026');

INSERT INTO vehicle_models (brand_id, code, name, model_year)
SELECT b.id, 'EXAMPLE-TOUR-2026', 'Tour Example', 2026
FROM brands b
WHERE b.name = 'Example Automotive'
  AND NOT EXISTS (SELECT 1 FROM vehicle_models WHERE code = 'EXAMPLE-TOUR-2026');

INSERT INTO vehicles (plate, model_id, purpose, daily_rental_rate, mileage, status)
SELECT 'DH0001', vm.id, 'RENTAL', 59.00, 0, 'AVAILABLE'
FROM vehicle_models vm
WHERE vm.code = 'DEMO-CITY-2026'
  AND NOT EXISTS (SELECT 1 FROM vehicles WHERE plate = 'DH0001');

INSERT INTO vehicles (plate, model_id, purpose, sale_price, mileage, status)
SELECT 'DH0002', vm.id, 'FOR_SALE', 24900.00, 1000, 'AVAILABLE'
FROM vehicle_models vm
WHERE vm.code = 'EXAMPLE-TOUR-2026'
  AND NOT EXISTS (SELECT 1 FROM vehicles WHERE plate = 'DH0002');
