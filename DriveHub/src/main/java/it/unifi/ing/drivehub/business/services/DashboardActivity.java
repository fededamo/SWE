package it.unifi.ing.drivehub.business.services;

import java.time.Instant;

public record DashboardActivity(Instant occurredAt, String description) { }
