package it.unifi.ing.drivehub.business.services;

import java.time.Instant;
import java.math.BigDecimal;

/** Amount is absent for activity without a monetary value. */
public record DashboardActivity(Instant occurredAt, String description, BigDecimal amount) {
    public DashboardActivity(Instant occurredAt, String description) { this(occurredAt, description, null); }
}
