package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

public final class Discount extends BaseEntity {
    private String name;
    private final Vehicle vehicle;
    private BigDecimal percentage;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private boolean enabled;

    public Discount(Long id, String name, Vehicle vehicle, BigDecimal percentage,
                    LocalDate startsOn, LocalDate endsOn, boolean enabled) {
        super(id);
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        replaceValues(name, percentage, startsOn, endsOn);
        this.enabled = enabled;
    }

    public static Discount create(String name, Vehicle vehicle, BigDecimal percentage,
                                  LocalDate startsOn, LocalDate endsOn) {
        return new Discount(null, name, vehicle, percentage, startsOn, endsOn, true);
    }

    public String name() {
        return name;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public BigDecimal percentage() {
        return percentage;
    }

    public LocalDate startsOn() {
        return startsOn;
    }

    public LocalDate endsOn() {
        return endsOn;
    }

    public boolean enabled() {
        return enabled;
    }

    public void disable() {
        enabled = false;
    }

    /** Reuses the one ER discount row associated with a vehicle. */
    public void replace(String name, BigDecimal percentage, LocalDate startsOn, LocalDate endsOn) {
        replaceValues(name, percentage, startsOn, endsOn);
        enabled = true;
    }

    public boolean appliesTo(Vehicle vehicle, LocalDate date) {
        Objects.requireNonNull(vehicle, "vehicle");
        Objects.requireNonNull(date, "date");
        boolean sameVehicle = sameEntity(this.vehicle, vehicle);
        return enabled && sameVehicle && !date.isBefore(startsOn) && !date.isAfter(endsOn);
    }

    public BigDecimal apply(BigDecimal price) {
        Objects.requireNonNull(price, "price");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(percentage.movePointLeft(2));
        return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean sameEntity(Vehicle first, Vehicle second) {
        return first.id() != null && second.id() != null
                ? first.id().equals(second.id())
                : first == second;
    }

    private void replaceValues(String newName, BigDecimal newPercentage,
                               LocalDate newStartsOn, LocalDate newEndsOn) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("discount name must not be blank");
        }
        Objects.requireNonNull(newPercentage, "percentage");
        if (newPercentage.signum() <= 0
                || newPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("discount percentage must be in (0, 100)");
        }
        Objects.requireNonNull(newStartsOn, "startsOn");
        Objects.requireNonNull(newEndsOn, "endsOn");
        if (newEndsOn.isBefore(newStartsOn)) {
            throw new IllegalArgumentException("discount end must not precede its start");
        }
        name = newName.trim();
        percentage = newPercentage;
        startsOn = newStartsOn;
        endsOn = newEndsOn;
    }
}
