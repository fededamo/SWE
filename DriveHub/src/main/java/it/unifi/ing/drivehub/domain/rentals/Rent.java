package it.unifi.ing.drivehub.domain.rentals;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Noleggio di un veicolo da parte di un cliente.
 */
public final class Rent {
    private Long id;
    private final long vehicleId;
    private final long clientId;
    private final BigDecimal amount;
    private final LocalDate start;
    private final LocalDate end;

    public Rent(Long id, long vehicleId, long clientId, BigDecimal amount,
                LocalDate start, LocalDate end) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (vehicleId <= 0) {
            throw new IllegalArgumentException("vehicleId must be positive");
        }
        if (clientId <= 0) {
            throw new IllegalArgumentException("clientId must be positive");
        }
        this.id = id;
        this.vehicleId = vehicleId;
        this.clientId = clientId;
        this.amount = Objects.requireNonNull(amount, "amount");
        if (this.amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end date cannot be before start date");
        }
    }

    public static Rent create(long vehicleId, long clientId, BigDecimal amount,
                              LocalDate start, LocalDate end) {
        return new Rent(null, vehicleId, clientId, amount, start, end);
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

    public long vehicleId() {
        return vehicleId;
    }

    public long clientId() {
        return clientId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rent other)) return false;
        if (id != null && other.id != null) {
            return Objects.equals(id, other.id);
        }
        return vehicleId == other.vehicleId && clientId == other.clientId &&
                Objects.equals(start, other.start) && Objects.equals(end, other.end);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(vehicleId, clientId, start, end);
    }
}
