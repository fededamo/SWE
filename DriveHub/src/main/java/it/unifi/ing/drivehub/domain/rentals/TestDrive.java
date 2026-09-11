package it.unifi.ing.drivehub.domain.rentals;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public final class TestDrive extends BaseEntity {
    private final User customer;
    private final Vehicle vehicle;
    private final LocalDateTime scheduledAt;
    private final Duration duration;
    private User salesman;
    private TestDriveStatus status;

    public TestDrive(Long id, User customer, User salesman, Vehicle vehicle, LocalDateTime scheduledAt,
                     Duration duration, TestDriveStatus status) {
        super(id);
        this.customer = Objects.requireNonNull(customer, "customer");
        requireStoredRole(customer, Role.CUSTOMER, "customer");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
        this.duration = Objects.requireNonNull(duration, "duration");
        if (duration.compareTo(Duration.ofMinutes(15)) < 0 || duration.compareTo(Duration.ofHours(4)) > 0) {
            throw new IllegalArgumentException("test drive duration must be between 15 minutes and 4 hours");
        }
        this.status = Objects.requireNonNull(status, "status");
        this.salesman = salesman;
        if (salesman != null) {
            requireStoredRole(salesman, Role.SALESMAN, "salesman");
        }
        if (status == TestDriveStatus.REQUESTED && salesman != null) {
            throw new IllegalArgumentException("a requested test drive must be unassigned");
        }
        if (requiresSalesman(status) && salesman == null) {
            throw new IllegalArgumentException("test drive status " + status + " requires a salesman");
        }
    }

    public static TestDrive request(User customer, Vehicle vehicle, LocalDateTime scheduledAt, Duration duration) {
        return new TestDrive(null, customer, null, vehicle, scheduledAt, duration, TestDriveStatus.REQUESTED);
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

    public LocalDateTime scheduledAt() {
        return scheduledAt;
    }

    public LocalDateTime endsAt() {
        return scheduledAt.plus(duration);
    }

    public Duration duration() {
        return duration;
    }

    public TestDriveStatus status() {
        return status;
    }

    public void confirm(User candidate) {
        candidate.requireRole(Role.SALESMAN);
        if (status != TestDriveStatus.REQUESTED || salesman != null) {
            throw new DomainRuleViolationException("test drive has already been assigned");
        }
        salesman = candidate;
        status = TestDriveStatus.CONFIRMED;
    }

    public void start(User actor) {
        requireOwner(actor);
        transition(TestDriveStatus.CONFIRMED, TestDriveStatus.IN_PROGRESS);
    }

    public void complete(User actor) {
        requireOwner(actor);
        transition(TestDriveStatus.IN_PROGRESS, TestDriveStatus.COMPLETED);
    }

    public void cancel(User actor) {
        Objects.requireNonNull(actor, "actor").requireRole(actor.role());
        if (status == TestDriveStatus.IN_PROGRESS || status == TestDriveStatus.COMPLETED
                || status == TestDriveStatus.CANCELLED) {
            throw new DomainRuleViolationException("test drive cannot be cancelled in status " + status);
        }
        boolean isCustomer = sameUser(customer, actor);
        boolean isOwner = salesman != null && sameUser(salesman, actor);
        if (!isCustomer && !isOwner) {
            throw new DomainRuleViolationException("only the customer or assigned salesman may cancel");
        }
        status = TestDriveStatus.CANCELLED;
    }

    private void requireOwner(User actor) {
        actor.requireRole(Role.SALESMAN);
        if (salesman == null || !sameUser(salesman, actor)) {
            throw new DomainRuleViolationException("test drive is owned by another salesman");
        }
    }

    private void transition(TestDriveStatus expected, TestDriveStatus next) {
        if (status != expected) {
            throw new DomainRuleViolationException(
                    "test drive transition requires " + expected + " but was " + status);
        }
        status = next;
    }

    private static boolean requiresSalesman(TestDriveStatus value) {
        return value == TestDriveStatus.CONFIRMED || value == TestDriveStatus.IN_PROGRESS
                || value == TestDriveStatus.COMPLETED;
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
}
