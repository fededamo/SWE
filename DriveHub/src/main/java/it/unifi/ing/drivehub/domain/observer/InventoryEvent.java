package it.unifi.ing.drivehub.domain.observer;

import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.time.Instant;
import java.util.Objects;

public record InventoryEvent(
        InventoryEventType type,
        Long vehicleId,
        String plate,
        VehicleStatus previousStatus,
        VehicleStatus currentStatus,
        Instant occurredAt
) {
    public InventoryEvent {
        Objects.requireNonNull(type, "type");
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("plate must not be blank");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
