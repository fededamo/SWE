package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Vehicle {
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z0-9]{5,10}$");

    private Long id;
    private final long modelId;
    private final String vin;
    private String licencePlate;
    private int kilometers;
    private BigDecimal price;
    private int year;
    private VehicleState state;

    public Vehicle(Long id, long modelId, String vin, String licencePlate,
                   int kilometers, BigDecimal price, int year, VehicleState state) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (modelId <= 0) {
            throw new IllegalArgumentException("modelId must be positive");
        }
        if (vin == null || vin.isBlank() || vin.length() > 17) {
            throw new IllegalArgumentException("VIN must be non-empty and at most 17 characters");
        }
        this.id = id;
        this.modelId = modelId;
        this.vin = vin.trim().toUpperCase(Locale.ROOT);
        this.licencePlate = normalizePlate(licencePlate);
        if (kilometers < 0) {
            throw new IllegalArgumentException("kilometers cannot be negative");
        }
        if (this.licencePlate == null && kilometers != 0) {
            throw new DomainRuleViolationException("Se un veicolo non ha targa, obbligatoriamente i chilometri devono essere a 0.");
        }
        this.kilometers = kilometers;
        this.price = Objects.requireNonNull(price, "price");
        if (this.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        int currentYear = LocalDate.now().getYear();
        if (year < 1900 || year > currentYear) {
            throw new IllegalArgumentException("Anno del veicolo non valido (" + year + "): deve essere compreso tra 1900 e " + currentYear);
        }
        this.year = year;
        this.state = Objects.requireNonNull(state, "state");
    }

    public static Vehicle create(long modelId, String vin, String licencePlate,
                                 int kilometers, BigDecimal price, int year, VehicleState state) {
        return new Vehicle(null, modelId, vin, licencePlate, kilometers, price, year, state);
    }

    public Long id() {
        return id;
    }

    public void assignId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (this.id != null && this.id != id) {
            throw new DomainRuleViolationException("entity id is already assigned");
        }
        this.id = id;
    }

    public long requireId() {
        if (id == null) {
            throw new DomainRuleViolationException("entity has not been persisted yet");
        }
        return id;
    }

    public long modelId() {
        return modelId;
    }

    public String vin() {
        return vin;
    }

    public String licencePlate() {
        return licencePlate;
    }

    public int kilometers() {
        return kilometers;
    }

    public BigDecimal price() {
        return price;
    }

    public int year() {
        return year;
    }

    public VehicleState state() {
        return state;
    }

    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainRuleViolationException("Price cannot be negative");
        }
        this.price = newPrice;
    }

    public void transitionTo(VehicleState newState) {
        Objects.requireNonNull(newState, "newState");
        if (this.state == newState) {
            return;
        }
        boolean valid = switch (this.state) {
            case WAITING -> newState == VehicleState.SALE || newState == VehicleState.SOLD;
            case SALE -> newState == VehicleState.BLOCKED || newState == VehicleState.RENT || newState == VehicleState.SOLD;
            case BLOCKED -> newState == VehicleState.SALE || newState == VehicleState.SOLD;
            case RENT -> newState == VehicleState.SALE;
            case SOLD -> false;
        };
        if (!valid) {
            throw new DomainRuleViolationException("Transizione di stato non valida da " + this.state + " a " + newState);
        }
        this.state = newState;
    }

    public void updateDetails(String newPlate, int newKm, BigDecimal newPrice, int newYear, VehicleState newState) {
        String normalized = normalizePlate(newPlate);
        if (newKm < 0) {
            throw new IllegalArgumentException("kilometers cannot be negative");
        }
        if (normalized == null && newKm != 0) {
            throw new DomainRuleViolationException("Se un veicolo non ha targa, obbligatoriamente i chilometri devono essere a 0.");
        }
        int currentYear = LocalDate.now().getYear();
        if (newYear < 1900 || newYear > currentYear) {
            throw new IllegalArgumentException("Anno del veicolo non valido (" + newYear + "): deve essere compreso tra 1900 e " + currentYear);
        }
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        this.licencePlate = normalized;
        this.kilometers = newKm;
        this.price = newPrice;
        this.year = newYear;
        this.state = Objects.requireNonNull(newState, "newState");
    }

    public void approve() {
        if (state != VehicleState.WAITING) {
            throw new DomainRuleViolationException("Cannot approve vehicle not in WAITING state");
        }
        this.state = VehicleState.SALE;
    }

    public void block() {
        if (state != VehicleState.SALE) {
            throw new DomainRuleViolationException("Cannot block vehicle not in SALE state");
        }
        this.state = VehicleState.BLOCKED;
    }

    public void unblock() {
        if (state != VehicleState.BLOCKED) {
            throw new DomainRuleViolationException("Cannot unblock vehicle not in BLOCKED state");
        }
        this.state = VehicleState.SALE;
    }

    private static String normalizePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        String normalized = plate.trim().toUpperCase(Locale.ROOT);
        if (!PLATE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid licence plate format: " + plate);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        return Objects.equals(vin, other.vin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vin);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", modelId=" + modelId +
                ", vin='" + vin + '\'' +
                ", licencePlate='" + licencePlate + '\'' +
                ", kilometers=" + kilometers +
                ", price=" + price +
                ", year=" + year +
                ", state=" + state +
                '}';
    }
}
