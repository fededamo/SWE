package it.unifi.ing.drivehub.domain.rentals;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Rental extends BaseEntity {
    private final User customer;
    private final Vehicle vehicle;
    private final LocalDate startsOn;
    private final LocalDate endsOn;
    private final BigDecimal totalPrice;
    private User salesman;
    private RentalStatus status;

    public Rental(Long id, User customer, User salesman, Vehicle vehicle, LocalDate startsOn,
                  LocalDate endsOn, BigDecimal totalPrice, RentalStatus status) {
        super(id);
        this.customer = Objects.requireNonNull(customer, "customer");
        requireStoredRole(customer, Role.CUSTOMER, "customer");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        if (!vehicle.purpose().supportsRental()) {
            throw new IllegalArgumentException("vehicle is not offered for rental");
        }
        this.startsOn = Objects.requireNonNull(startsOn, "startsOn");
        this.endsOn = Objects.requireNonNull(endsOn, "endsOn");
        if (!endsOn.isAfter(startsOn)) {
            throw new IllegalArgumentException("rental end must be after its start");
        }
        this.totalPrice = positive(totalPrice, "totalPrice");
        this.status = Objects.requireNonNull(status, "status");
        this.salesman = salesman;
        if (salesman != null) {
            requireStoredRole(salesman, Role.SALESMAN, "salesman");
        }
        if (status == RentalStatus.REQUESTED && salesman != null) {
            throw new IllegalArgumentException("a requested rental must be unassigned");
        }
        if (requiresSalesman(status) && salesman == null) {
            throw new IllegalArgumentException("rental status " + status + " requires a salesman");
        }
    }

    public static Rental request(User customer, Vehicle vehicle, LocalDate startsOn,
                                 LocalDate endsOn, BigDecimal totalPrice) {
        return new Rental(null, customer, null, vehicle, startsOn, endsOn, totalPrice, RentalStatus.REQUESTED);
    }

    public User customer() {
        return customer;
    }

    public User salesman() {
        return salesman;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public LocalDate startsOn() {
        return startsOn;
    }

    public LocalDate endsOn() {
        return endsOn;
    }

    public BigDecimal totalPrice() {
        return totalPrice;
    }

    public RentalStatus status() {
        return status;
    }

    public void claim(User candidate) {
        candidate.requireRole(Role.SALESMAN);
        if (salesman != null || status != RentalStatus.REQUESTED) {
            throw new DomainRuleViolationException("rental has already been assigned");
        }
        salesman = candidate;
        status = RentalStatus.ASSIGNED;
    }

    public void confirm(User actor) {
        requireOwner(actor);
        transition(RentalStatus.ASSIGNED, RentalStatus.CONFIRMED);
    }

    public void start(User actor) {
        requireOwner(actor);
        transition(RentalStatus.CONFIRMED, RentalStatus.ACTIVE);
    }

    public void complete(User actor) {
        requireOwner(actor);
        transition(RentalStatus.ACTIVE, RentalStatus.COMPLETED);
    }

    public void cancel(User actor) {
        if (status == RentalStatus.ACTIVE || status == RentalStatus.COMPLETED || status == RentalStatus.CANCELLED) {
            throw new DomainRuleViolationException("rental cannot be cancelled in status " + status);
        }
        boolean isCustomer = sameUser(customer, actor);
        boolean isOwner = salesman != null && sameUser(salesman, actor);
        if (!isCustomer && !isOwner) {
            throw new DomainRuleViolationException("only the customer or assigned salesman may cancel");
        }
        status = RentalStatus.CANCELLED;
    }

    public boolean isOwnedBy(User candidate) {
        return salesman != null && sameUser(salesman, candidate);
    }

    private void requireOwner(User actor) {
        actor.requireRole(Role.SALESMAN);
        if (!isOwnedBy(actor)) {
            throw new DomainRuleViolationException("rental is owned by another salesman");
        }
    }

    private void transition(RentalStatus expected, RentalStatus next) {
        if (status != expected) {
            throw new DomainRuleViolationException(
                    "rental transition requires " + expected + " but was " + status);
        }
        status = next;
    }

    private static boolean requiresSalesman(RentalStatus value) {
        return value == RentalStatus.ASSIGNED || value == RentalStatus.CONFIRMED
                || value == RentalStatus.ACTIVE || value == RentalStatus.COMPLETED;
    }

    private static boolean sameUser(User first, User second) {
        if (first == null || second == null) {
            return false;
        }
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
}
