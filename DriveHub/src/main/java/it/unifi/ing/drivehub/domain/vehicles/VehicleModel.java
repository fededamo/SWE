package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.BaseEntity;

import java.util.Objects;

public final class VehicleModel extends BaseEntity {
    private final Brand brand;
    private final String code;
    private final String name;
    private final int modelYear;

    public VehicleModel(Long id, Brand brand, String code, String name, int modelYear) {
        super(id);
        this.brand = Objects.requireNonNull(brand, "brand");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("model code must not be blank");
        }
        this.code = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("model name must not be blank");
        }
        if (modelYear < 1886 || modelYear > 2200) {
            throw new IllegalArgumentException("invalid model year");
        }
        this.name = name.trim();
        this.modelYear = modelYear;
    }

    public static VehicleModel create(Brand brand, String code, String name, int modelYear) {
        return new VehicleModel(null, brand, code, name, modelYear);
    }

    public Brand brand() {
        return brand;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int modelYear() {
        return modelYear;
    }
}
