package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Richiesta di acquisto (deposit > 0) o di vendita veicolo del cliente (deposit < 0).
 */
public final class Request {
    private Long id;
    private final long vehicleId;
    private final long clientId;
    private Long salesmanId;
    private final BigDecimal deposit;
    private final RequestType type;
    private LocalDate expDate;

    public Request(Long id, long vehicleId, long clientId, Long salesmanId, BigDecimal deposit, LocalDate expDate, RequestType type) {
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
        this.salesmanId = salesmanId;
        this.deposit = Objects.requireNonNull(deposit, "deposit");
        if (this.deposit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("deposit cannot be zero");
        }
        this.type = type != null ? type : resolveType(deposit);
        this.expDate = expDate;
    }

    public Request(Long id, long vehicleId, long clientId, Long salesmanId, BigDecimal deposit, LocalDate expDate) {
        this(id, vehicleId, clientId, salesmanId, deposit, expDate, resolveType(deposit));
    }

    private static RequestType resolveType(BigDecimal deposit) {
        return (deposit != null && deposit.compareTo(BigDecimal.ZERO) < 0)
                ? RequestType.CLIENT_PROPOSAL
                : RequestType.CLIENT_PURCHASE;
    }

    public static Request createPurchase(long vehicleId, long clientId, BigDecimal deposit) {
        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase deposit must be strictly positive");
        }
        return new Request(null, vehicleId, clientId, null, deposit, null);
    }

    public static Request createSaleProposal(long vehicleId, long clientId, BigDecimal estimatedValue) {
        if (estimatedValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Estimated value must be strictly positive");
        }
        return new Request(null, vehicleId, clientId, null, estimatedValue.negate(), null);
    }

    public static Request create(long vehicleId, long clientId, BigDecimal deposit) {
        return new Request(null, vehicleId, clientId, null, deposit, null);
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

    public Long salesmanId() {
        return salesmanId;
    }

    public BigDecimal deposit() {
        return deposit;
    }

    public LocalDate expDate() {
        return expDate;
    }

    public RequestType type() {
        return type;
    }

    public boolean isPurchase() {
        return type == RequestType.CLIENT_PURCHASE;
    }

    public boolean isSaleProposal() {
        return type == RequestType.CLIENT_PROPOSAL;
    }

    public void authorize(long salesmanId, LocalDate expDate) {
        this.salesmanId = salesmanId;
        this.expDate = Objects.requireNonNull(expDate, "expDate");
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public void setSalesmanId(Long salesmanId) {
        this.salesmanId = salesmanId;
    }

    public void unlock(LocalDate expDate) {
        this.expDate = Objects.requireNonNull(expDate, "expDate");
    }

    public boolean isUnlocked() {
        return expDate != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request other)) return false;
        if (id != null && other.id != null) {
            return Objects.equals(id, other.id);
        }
        return vehicleId == other.vehicleId && clientId == other.clientId &&
                Objects.equals(deposit, other.deposit) && Objects.equals(expDate, other.expDate);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(vehicleId, clientId, deposit, expDate);
    }
}
