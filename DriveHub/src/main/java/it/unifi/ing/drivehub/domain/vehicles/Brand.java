package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.BaseEntity;

public final class Brand extends BaseEntity {
    private final String name;

    public Brand(Long id, String name) {
        super(id);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("brand name must not be blank");
        }
        this.name = name.trim();
    }

    public static Brand create(String name) {
        return new Brand(null, name);
    }

    public String name() {
        return name;
    }
}
