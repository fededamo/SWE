package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.util.Objects;

public final class SaleOrder extends BaseEntity {
    private final User customer;
    private final Vehicle vehicle;
    private final BigDecimal totalPrice;
    private final BigDecimal requiredDeposit;
    private User salesman;
    private BigDecimal paidAmount;
    private SaleOrderStatus status;

    public SaleOrder(Long id, User customer, User salesman, Vehicle vehicle, BigDecimal totalPrice,
                     BigDecimal requiredDeposit, BigDecimal paidAmount, SaleOrderStatus status) {
        super(id);
        this.customer = Objects.requireNonNull(customer, "customer");
        requireStoredRole(customer, Role.CUSTOMER, "customer");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        if (!vehicle.purpose().supportsSale()) {
            throw new IllegalArgumentException("vehicle is not offered for sale");
        }
        this.totalPrice = positive(totalPrice, "totalPrice");
        this.requiredDeposit = positive(requiredDeposit, "requiredDeposit");
        if (requiredDeposit.compareTo(totalPrice) > 0) {
            throw new IllegalArgumentException("required deposit cannot exceed total price");
        }
        this.paidAmount = nonNegative(paidAmount, "paidAmount");
        if (paidAmount.compareTo(totalPrice) > 0) {
            throw new IllegalArgumentException("paid amount cannot exceed total price");
        }
        this.status = Objects.requireNonNull(status, "status");
        this.salesman = salesman;
        if (salesman != null) {
            requireStoredRole(salesman, Role.SALESMAN, "salesman");
        }
    }

    public static SaleOrder reserve(User customer, Vehicle vehicle, BigDecimal totalPrice,
                                    BigDecimal requiredDeposit) {
        vehicle.reserveForSale();
        return new SaleOrder(null, customer, null, vehicle, totalPrice, requiredDeposit,
                BigDecimal.ZERO, SaleOrderStatus.RESERVED);
    }

    public User customer() { return customer; }
    public User salesman() { return salesman; }
    public Vehicle vehicle() { return vehicle; }
    public BigDecimal totalPrice() { return totalPrice; }
    public BigDecimal requiredDeposit() { return requiredDeposit; }
    public BigDecimal paidAmount() { return paidAmount; }
    public SaleOrderStatus status() { return status; }

    public void assignSalesman(User candidate) {
        candidate.requireRole(Role.SALESMAN);
        if (salesman != null && !sameUser(salesman, candidate)) {
            throw new DomainRuleViolationException("sale order is already assigned to another salesman");
        }
        if (status == SaleOrderStatus.CANCELLED || status == SaleOrderStatus.COMPLETED) {
            throw new DomainRuleViolationException("terminal sale order cannot be assigned");
        }
        salesman = candidate;
    }

    public void recordPayment(BigDecimal amount) {
        positive(amount, "amount");
        if (status == SaleOrderStatus.CANCELLED || status == SaleOrderStatus.COMPLETED
                || status == SaleOrderStatus.PAID) {
            throw new DomainRuleViolationException("sale order does not accept another payment");
        }
        BigDecimal updated = paidAmount.add(amount);
        if (updated.compareTo(totalPrice) > 0) {
            throw new DomainRuleViolationException("payment would exceed order total");
        }
        paidAmount = updated;
        if (paidAmount.compareTo(totalPrice) == 0) {
            status = SaleOrderStatus.PAID;
        } else if (paidAmount.compareTo(requiredDeposit) >= 0) {
            status = SaleOrderStatus.DEPOSIT_PAID;
        }
    }

    public void complete(User actor) {
        requireOwner(actor);
        if (status != SaleOrderStatus.PAID) {
            throw new DomainRuleViolationException("only a fully paid order can be completed");
        }
        vehicle.markSold();
        status = SaleOrderStatus.COMPLETED;
    }

    public void cancel(User actor) {
        boolean customerActor = sameUser(customer, actor);
        boolean salesmanActor = salesman != null && sameUser(salesman, actor);
        if (!customerActor && !salesmanActor) {
            throw new DomainRuleViolationException("only customer or assigned salesman may cancel the order");
        }
        if (status == SaleOrderStatus.PAID || status == SaleOrderStatus.COMPLETED
                || status == SaleOrderStatus.CANCELLED) {
            throw new DomainRuleViolationException("sale order cannot be cancelled in status " + status);
        }
        if (vehicle.status() == VehicleStatus.RESERVED) {
            vehicle.releaseSaleReservation();
        }
        status = SaleOrderStatus.CANCELLED;
    }

    private void requireOwner(User actor) {
        actor.requireRole(Role.SALESMAN);
        if (salesman == null || !sameUser(salesman, actor)) {
            throw new DomainRuleViolationException("sale order is owned by another salesman");
        }
    }

    private static boolean sameUser(User first, User second) {
        return first.id() != null && second.id() != null
                ? first.id().equals(second.id())
                : first == second;
    }

    private static void requireStoredRole(User user, Role role, String field) {
        if (!user.hasRole(role)) {
            throw new IllegalArgumentException(field + " must have role " + role);
        }
    }

    private static BigDecimal positive(BigDecimal amount, String field) {
        Objects.requireNonNull(amount, field);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return amount;
    }

    private static BigDecimal nonNegative(BigDecimal amount, String field) {
        Objects.requireNonNull(amount, field);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return amount;
    }
}
