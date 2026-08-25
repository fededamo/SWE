package it.unifi.ing.drivehub.business.strategies;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Base price with the single best applicable promotion; promotions never stack. */
public final class StandardPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal salePrice(Vehicle vehicle, List<Discount> discounts, LocalDate date) {
        Objects.requireNonNull(vehicle, "vehicle");
        if (!vehicle.purpose().supportsSale()) {
            throw new DomainRuleViolationException("vehicle is not offered for sale");
        }
        return discounted(vehicle.salePrice(), vehicle, discounts, date);
    }

    @Override
    public BigDecimal rentalPrice(Vehicle vehicle, long days, List<Discount> discounts, LocalDate date) {
        Objects.requireNonNull(vehicle, "vehicle");
        if (!vehicle.purpose().supportsRental()) {
            throw new DomainRuleViolationException("vehicle is not offered for rental");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("rental days must be positive");
        }
        BigDecimal base = vehicle.dailyRentalRate().multiply(BigDecimal.valueOf(days));
        return discounted(base, vehicle, discounts, date);
    }

    private static BigDecimal discounted(BigDecimal base, Vehicle vehicle, List<Discount> discounts,
                                         LocalDate date) {
        Objects.requireNonNull(discounts, "discounts");
        Objects.requireNonNull(date, "date");
        return discounts.stream()
                .filter(discount -> discount.appliesTo(vehicle, date))
                .max(Comparator.comparing(Discount::percentage))
                .map(discount -> discount.apply(base))
                .orElseGet(() -> base.setScale(2, RoundingMode.HALF_UP));
    }
}
