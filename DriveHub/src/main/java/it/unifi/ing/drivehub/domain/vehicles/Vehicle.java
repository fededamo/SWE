package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.observer.InventoryEvent;
import it.unifi.ing.drivehub.domain.observer.InventoryEventType;
import it.unifi.ing.drivehub.domain.observer.InventoryObserver;
import it.unifi.ing.drivehub.domain.observer.InventorySubject;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class Vehicle extends BaseEntity implements InventorySubject {
    private static final Pattern PLATE = Pattern.compile("^[A-Z0-9]{5,10}$");

    private final String plate;
    private final VehicleModel model;
    private final VehiclePurpose purpose;
    private BigDecimal salePrice;
    private BigDecimal dailyRentalRate;
    private final long mileage;
    private final Clock clock;
    private final List<InventoryObserver> observers = new CopyOnWriteArrayList<>();
    private VehicleStatus status;

    public Vehicle(Long id, String plate, VehicleModel model, VehiclePurpose purpose, BigDecimal salePrice,
                   BigDecimal dailyRentalRate, long mileage, VehicleStatus status) {
        this(id, plate, model, purpose, salePrice, dailyRentalRate, mileage, status, Clock.systemUTC());
    }

    Vehicle(Long id, String plate, VehicleModel model, VehiclePurpose purpose, BigDecimal salePrice,
            BigDecimal dailyRentalRate, long mileage, VehicleStatus status, Clock clock) {
        super(id);
        this.plate = normalizePlate(plate);
        this.model = Objects.requireNonNull(model, "model");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.salePrice = validatePrice(salePrice, "salePrice", purpose.supportsSale());
        this.dailyRentalRate = validatePrice(dailyRentalRate, "dailyRentalRate", purpose.supportsRental());
        if (mileage < 0) {
            throw new IllegalArgumentException("mileage must not be negative");
        }
        this.mileage = mileage;
        this.status = Objects.requireNonNull(status, "status");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static Vehicle create(String plate, VehicleModel model, VehiclePurpose purpose,
                                 BigDecimal salePrice, BigDecimal dailyRentalRate, long mileage) {
        return new Vehicle(null, plate, model, purpose, salePrice, dailyRentalRate, mileage,
                VehicleStatus.AVAILABLE);
    }

    public String plate() {
        return plate;
    }

    public VehicleModel model() {
        return model;
    }

    public VehiclePurpose purpose() {
        return purpose;
    }

    public BigDecimal salePrice() {
        return salePrice;
    }

    public BigDecimal dailyRentalRate() {
        return dailyRentalRate;
    }

    public long mileage() {
        return mileage;
    }

    public void updateSalePrice(it.unifi.ing.drivehub.domain.users.User manager, BigDecimal newPrice) {
        Objects.requireNonNull(manager, "manager")
                .requireRole(it.unifi.ing.drivehub.domain.users.Role.MANAGER);
        requirePurpose(purpose.supportsSale(), "vehicle is not offered for sale");
        salePrice = validatePrice(newPrice, "salePrice", true);
    }

    public void updateDailyRentalRate(it.unifi.ing.drivehub.domain.users.User manager, BigDecimal newRate) {
        Objects.requireNonNull(manager, "manager")
                .requireRole(it.unifi.ing.drivehub.domain.users.Role.MANAGER);
        requirePurpose(purpose.supportsRental(), "vehicle is not offered for rental");
        dailyRentalRate = validatePrice(newRate, "dailyRentalRate", true);
    }

    public VehicleStatus status() {
        return status;
    }

    public boolean isAvailableForSale() {
        return purpose.supportsSale() && status == VehicleStatus.AVAILABLE;
    }

    public boolean isAvailableForRental() {
        return purpose.supportsRental() && status == VehicleStatus.AVAILABLE;
    }

    public void reserveForSale() {
        requirePurpose(purpose.supportsSale(), "vehicle is not offered for sale");
        transition(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED);
    }

    public void releaseSaleReservation() {
        transition(VehicleStatus.RESERVED, VehicleStatus.AVAILABLE);
    }

    public void markSold() {
        transition(VehicleStatus.RESERVED, VehicleStatus.SOLD);
    }

    public void startRental() {
        requirePurpose(purpose.supportsRental(), "vehicle is not offered for rental");
        transition(VehicleStatus.AVAILABLE, VehicleStatus.RENTED);
    }

    public void finishRental() {
        transition(VehicleStatus.RENTED, VehicleStatus.AVAILABLE);
    }

    public void startTestDrive() {
        transition(VehicleStatus.AVAILABLE, VehicleStatus.TEST_DRIVE);
    }

    public void finishTestDrive() {
        transition(VehicleStatus.TEST_DRIVE, VehicleStatus.AVAILABLE);
    }

    public void sendToMaintenance() {
        transition(VehicleStatus.AVAILABLE, VehicleStatus.MAINTENANCE);
    }

    public void returnFromMaintenance() {
        transition(VehicleStatus.MAINTENANCE, VehicleStatus.AVAILABLE);
    }

    @Override
    public void subscribe(InventoryObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    @Override
    public void unsubscribe(InventoryObserver observer) {
        observers.remove(observer);
    }

    private void transition(VehicleStatus expected, VehicleStatus next) {
        if (status != expected) {
            throw new DomainRuleViolationException(
                    "vehicle transition requires " + expected + " but was " + status);
        }
        VehicleStatus previous = status;
        status = next;
        InventoryEvent event = new InventoryEvent(InventoryEventType.STATUS_CHANGED, id(), plate,
                previous, next, Instant.now(clock));
        observers.forEach(observer -> observer.onInventoryEvent(event));
    }

    private static void requirePurpose(boolean condition, String message) {
        if (!condition) {
            throw new DomainRuleViolationException(message);
        }
    }

    public static String normalizePlate(String plate) {
        if (plate == null) {
            throw new IllegalArgumentException("plate must not be null");
        }
        String normalized = plate.replaceAll("[ -]", "").toUpperCase(Locale.ROOT);
        if (!PLATE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("plate must contain 5 to 10 alphanumeric characters");
        }
        return normalized;
    }

    private static BigDecimal validatePrice(BigDecimal value, String field, boolean required) {
        if (!required) {
            if (value != null) {
                throw new IllegalArgumentException(field + " is not applicable to this vehicle purpose");
            }
            return null;
        }
        Objects.requireNonNull(value, field);
        if (value.signum() < 0 || (required && value.signum() == 0)) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
