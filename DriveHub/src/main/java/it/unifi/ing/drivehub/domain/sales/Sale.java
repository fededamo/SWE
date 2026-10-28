package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro compravendite:
 * 1. Vendita al cliente: amount > 0, vehicleId != null, salesmanId != null.
 * 2. Acquisto da cliente: amount < 0 (esborso aziendale), vehicleId != null, salesmanId != null.
 * 3. Penale mancato saldo: amount > 0, vehicleId != null, salesmanId == null.
 */
public final class Sale {
    private Long id;
    private final Long vehicleId;
    private final Long salesmanId;
    private final long clientId;
    private final BigDecimal amount;
    private final LocalDateTime date;
    private final SaleType type;

    public Sale(Long id, Long vehicleId, Long salesmanId, long clientId, BigDecimal amount, LocalDateTime date, SaleType type) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (clientId <= 0) {
            throw new IllegalArgumentException("clientId must be positive");
        }
        this.id = id;
        this.vehicleId = vehicleId;
        this.salesmanId = salesmanId;
        this.clientId = clientId;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.date = Objects.requireNonNull(date, "date");
        this.type = type != null ? type : resolveType(salesmanId, amount);
        validateInvariants();
    }

    public Sale(Long id, Long vehicleId, Long salesmanId, long clientId, BigDecimal amount, LocalDateTime date) {
        this(id, vehicleId, salesmanId, clientId, amount, date, resolveType(salesmanId, amount));
    }

    private static SaleType resolveType(Long salesmanId, BigDecimal amount) {
        if (salesmanId == null) {
            return SaleType.CANCELLATION_PENALTY;
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            return SaleType.DEALERSHIP_ACQUISITION;
        }
        return SaleType.CLIENT_SALE;
    }

    private void validateInvariants() {
        switch (type) {
            case CANCELLATION_PENALTY -> {
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new DomainRuleViolationException("Penalty amount must be positive");
                }
            }
            case DEALERSHIP_ACQUISITION -> {
                if (salesmanId == null) {
                    throw new DomainRuleViolationException("Acquisition requires an approving salesman");
                }
            }
            case CLIENT_SALE -> {
                if (salesmanId == null) {
                    throw new DomainRuleViolationException("Sale requires a salesman");
                }
            }
        }
    }

    public static Sale record(Long vehicleId, Long salesmanId, long clientId, BigDecimal amount, LocalDateTime date) {
        return new Sale(null, vehicleId, salesmanId, clientId, amount, date);
    }

    public static Sale record(Long vehicleId, Long salesmanId, long clientId, BigDecimal amount, LocalDate date) {
        return new Sale(null, vehicleId, salesmanId, clientId, amount, date.atStartOfDay());
    }

    public static Sale penalty(Long vehicleId, long clientId, BigDecimal amount, LocalDateTime date) {
        return new Sale(null, vehicleId, null, clientId, amount, date, SaleType.CANCELLATION_PENALTY);
    }

    public static Sale recordAcquisition(Long vehicleId, Long salesmanId, long clientId, BigDecimal payout, LocalDateTime date) {
        BigDecimal signedAmount = payout.compareTo(BigDecimal.ZERO) > 0 ? payout.negate() : payout;
        return new Sale(null, vehicleId, salesmanId, clientId, signedAmount, date, SaleType.DEALERSHIP_ACQUISITION);
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

    public Long vehicleId() {
        return vehicleId;
    }

    public Long salesmanId() {
        return salesmanId;
    }

    public long clientId() {
        return clientId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public LocalDateTime date() {
        return date;
    }

    public LocalDate localDate() {
        return date.toLocalDate();
    }

    public SaleType type() {
        return type;
    }

    public boolean isPenalty() {
        return type == SaleType.CANCELLATION_PENALTY;
    }

    public boolean isDefaultFailure() {
        return isPenalty();
    }

    public boolean isAcquisition() {
        return type == SaleType.DEALERSHIP_ACQUISITION;
    }

    public boolean isClientSale() {
        return type == SaleType.CLIENT_SALE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sale other)) return false;
        if (id != null && other.id != null) {
            return Objects.equals(id, other.id);
        }
        return clientId == other.clientId && Objects.equals(vehicleId, other.vehicleId) &&
                Objects.equals(salesmanId, other.salesmanId) && Objects.equals(amount, other.amount) &&
                Objects.equals(date, other.date);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(vehicleId, salesmanId, clientId, amount, date);
    }
}
