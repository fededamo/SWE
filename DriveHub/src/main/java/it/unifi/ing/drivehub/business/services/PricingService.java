package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.business.strategies.PricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class PricingService {
    private final TransactionRunner transactions;
    private final PricingStrategy pricing;

    public PricingService(DaoFactory daoFactory, PricingStrategy pricing) {
        this.transactions = new TransactionRunner(daoFactory);
        this.pricing = Objects.requireNonNull(pricing, "pricing");
    }

    public BigDecimal quoteSale(long vehicleId, LocalDate date) {
        return transactions.execute(unit -> {
            Vehicle vehicle = unit.vehicles().findById(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            return pricing.salePrice(vehicle, unit.discounts().findActiveOn(date), date);
        });
    }

    /** Demonstration policy A-10: reserve with ten percent or pay the entire quote. */
    public BigDecimal quoteSalePayment(long vehicleId, boolean fullPurchase, LocalDate date) {
        BigDecimal total = quoteSale(vehicleId, date);
        return fullPurchase ? total : requiredSaleDeposit(total);
    }

    public static BigDecimal requiredSaleDeposit(BigDecimal total) {
        Objects.requireNonNull(total, "total");
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("sale total must be positive");
        }
        return total.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                .max(new BigDecimal("0.01")).min(total);
    }

    public BigDecimal quoteRental(long vehicleId, long days, LocalDate date) {
        return transactions.execute(unit -> {
            Vehicle vehicle = unit.vehicles().findById(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            return pricing.rentalPrice(vehicle, days, unit.discounts().findActiveOn(date), date);
        });
    }

    public Discount applyDiscount(long managerId, String name, long vehicleId, BigDecimal percentage,
                                  LocalDate startsOn, LocalDate endsOn) {
        return transactions.execute(unit -> {
            requireManager(unit.users().findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("manager", managerId)));
            Vehicle vehicle = unit.vehicles().findByIdForUpdate(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            Discount existing = unit.discounts().findAll().stream()
                    .filter(discount -> discount.vehicle().requireId() == vehicleId)
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                return unit.discounts().save(
                        Discount.create(name, vehicle, percentage, startsOn, endsOn));
            }
            existing.replace(name, percentage, startsOn, endsOn);
            unit.discounts().update(existing);
            return existing;
        });
    }

    public void removeDiscount(long managerId, long discountId) {
        transactions.execute(unit -> {
            requireManager(unit.users().findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("manager", managerId)));
            Discount discount = unit.discounts().findById(discountId)
                    .orElseThrow(() -> new EntityNotFoundException("discount", discountId));
            unit.vehicles().findByIdForUpdate(discount.vehicle().requireId()).orElseThrow();
            discount = unit.discounts().findById(discountId).orElseThrow();
            discount.disable();
            unit.discounts().update(discount);
            return null;
        });
    }

    public Vehicle updateSalePrice(long managerId, long vehicleId, BigDecimal price) {
        return updateVehiclePrice(managerId, vehicleId, price, true);
    }

    public Vehicle updateDailyRentalRate(long managerId, long vehicleId, BigDecimal rate) {
        return updateVehiclePrice(managerId, vehicleId, rate, false);
    }

    public List<Discount> discounts() {
        return transactions.execute(unit -> List.copyOf(unit.discounts().findAll()));
    }

    private Vehicle updateVehiclePrice(long managerId, long vehicleId, BigDecimal amount, boolean sale) {
        return transactions.execute(unit -> {
            User manager = unit.users().findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("manager", managerId));
            requireManager(manager);
            Vehicle vehicle = unit.vehicles().findByIdForUpdate(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            if (sale) {
                vehicle.updateSalePrice(manager, amount);
            } else {
                vehicle.updateDailyRentalRate(manager, amount);
            }
            unit.vehicles().update(vehicle);
            return vehicle;
        });
    }

    private static void requireManager(User user) {
        user.requireRole(Role.MANAGER);
    }
}
