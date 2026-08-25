package it.unifi.ing.drivehub.business.strategies;

import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PricingStrategy {
    BigDecimal salePrice(Vehicle vehicle, List<Discount> discounts, LocalDate date);

    BigDecimal rentalPrice(Vehicle vehicle, long days, List<Discount> discounts, LocalDate date);
}
