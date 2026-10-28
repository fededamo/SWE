package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Model {
    private Long id;
    private final String brandName;
    private final String name;
    private final BigDecimal releasePrice;
    private final int releaseYear;

    public Model(Long id, String brandName, String name, BigDecimal releasePrice, int releaseYear) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (brandName == null || brandName.isBlank()) {
            throw new IllegalArgumentException("brandName must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("model name must not be blank");
        }
        this.id = id;
        this.brandName = brandName.trim();
        this.name = name.trim();
        this.releasePrice = Objects.requireNonNull(releasePrice, "releasePrice");
        if (this.releasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("releasePrice cannot be negative");
        }
        int currentYear = LocalDate.now().getYear();
        if (releaseYear < 1900 || releaseYear > currentYear) {
            throw new IllegalArgumentException("Anno di rilascio non valido (" + releaseYear + "): deve essere compreso tra 1900 e " + currentYear);
        }
        this.releaseYear = releaseYear;
    }

    public static Model create(String brandName, String name, BigDecimal releasePrice, int releaseYear) {
        return new Model(null, brandName, name, releasePrice, releaseYear);
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

    public String brandName() {
        return brandName;
    }

    public String name() {
        return name;
    }

    public BigDecimal releasePrice() {
        return releasePrice;
    }

    public int releaseYear() {
        return releaseYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Model other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return brandName + " " + name + " (" + releaseYear + ")";
    }
}
