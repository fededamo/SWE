package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.rentals.TestDriveStatus;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class TestDriveService {
    private final TransactionRunner transactions;
    private final Clock clock;

    public TestDriveService(DaoFactory daoFactory, Clock clock) {
        this.transactions = new TransactionRunner(daoFactory);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TestDriveService(DaoFactory daoFactory) {
        this(daoFactory, Clock.systemDefaultZone());
    }

    /** UC-C-TEST: books a test drive, initially awaiting salesman confirmation. */
    public TestDrive book(long customerId, long vehicleId, LocalDateTime scheduledAt, Duration duration) {
        return transactions.execute(unit -> {
            User customer = unit.users().findByIdForUpdate(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("customer", customerId));
            customer.requireRole(Role.CUSTOMER);
            Vehicle vehicle = unit.vehicles().findByIdForUpdate(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            if (!vehicle.isAvailableForSale()) {
                throw new ConflictException("only an available sale vehicle can be test-driven");
            }
            if (!scheduledAt.isAfter(LocalDateTime.now(clock))) {
                throw new IllegalArgumentException("test drive must be scheduled in the future");
            }
            LocalDateTime endsAt = scheduledAt.plus(duration);
            boolean customerConflict = unit.testDrives().findByCustomer(customerId).stream()
                    .filter(booking -> booking.status() != TestDriveStatus.CANCELLED
                            && booking.status() != TestDriveStatus.COMPLETED)
                    .anyMatch(booking -> booking.scheduledAt().isBefore(endsAt)
                            && scheduledAt.isBefore(booking.endsAt()));
            if (customerConflict) {
                throw new ConflictException("customer already has a test drive in that time slot");
            }
            if (unit.testDrives().existsOverlapping(vehicleId, scheduledAt, endsAt)) {
                throw new ConflictException("vehicle already has a test drive in that time slot");
            }
            return unit.testDrives().save(TestDrive.request(customer, vehicle, scheduledAt, duration));
        });
    }

    public TestDrive confirm(long testDriveId, long salesmanId) {
        return transactions.execute(unit -> {
            User salesman = unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId));
            salesman.requireRole(Role.SALESMAN);
            if (!unit.testDrives().confirmIfUnassigned(testDriveId, salesmanId)) {
                throw new ConflictException("test drive was already taken by another salesman");
            }
            return unit.testDrives().findById(testDriveId)
                    .orElseThrow(() -> new EntityNotFoundException("test drive", testDriveId));
        });
    }

    public TestDrive start(long testDriveId, long salesmanId) {
        return transactions.execute(unit -> {
            TestDrive booking = requireBooking(unit.testDrives().findByIdForUpdate(testDriveId), testDriveId);
            User salesman = requireUser(unit.users().findById(salesmanId), salesmanId);
            booking.start(salesman);
            booking.vehicle().startTestDrive();
            if (!unit.vehicles().updateStatusIfCurrent(booking.vehicle().requireId(),
                    VehicleStatus.AVAILABLE, VehicleStatus.TEST_DRIVE)) {
                throw new ConflictException("vehicle is no longer available");
            }
            unit.testDrives().update(booking);
            return booking;
        });
    }

    public TestDrive complete(long testDriveId, long salesmanId) {
        return transactions.execute(unit -> {
            TestDrive booking = requireBooking(unit.testDrives().findByIdForUpdate(testDriveId), testDriveId);
            User salesman = requireUser(unit.users().findById(salesmanId), salesmanId);
            booking.complete(salesman);
            booking.vehicle().finishTestDrive();
            if (!unit.vehicles().updateStatusIfCurrent(booking.vehicle().requireId(),
                    VehicleStatus.TEST_DRIVE, VehicleStatus.AVAILABLE)) {
                throw new ConflictException("vehicle test-drive state changed concurrently");
            }
            unit.testDrives().update(booking);
            return booking;
        });
    }

    public TestDrive cancel(long testDriveId, long actorId) {
        return transactions.execute(unit -> {
            TestDrive booking = requireBooking(unit.testDrives().findByIdForUpdate(testDriveId), testDriveId);
            User actor = requireUser(unit.users().findById(actorId), actorId);
            booking.cancel(actor);
            unit.testDrives().update(booking);
            return booking;
        });
    }

    public List<TestDrive> bookingsForCustomer(long customerId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(customerId), customerId).requireRole(Role.CUSTOMER);
            return List.copyOf(unit.testDrives().findByCustomer(customerId));
        });
    }

    public List<TestDrive> bookingsForSalesman(long salesmanId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(salesmanId), salesmanId).requireRole(Role.SALESMAN);
            return List.copyOf(unit.testDrives().findBySalesman(salesmanId));
        });
    }

    public List<TestDrive> unassignedBookings(long salesmanId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(salesmanId), salesmanId).requireRole(Role.SALESMAN);
            return List.copyOf(unit.testDrives().findUnassigned());
        });
    }

    /** Unassigned requests plus bookings already owned by this salesman. */
    public List<TestDrive> manageableBookings(long salesmanId) {
        return transactions.execute(unit -> {
            User salesman = requireUser(unit.users().findById(salesmanId), salesmanId);
            salesman.requireRole(Role.SALESMAN);
            java.util.ArrayList<TestDrive> result = new java.util.ArrayList<>(unit.testDrives().findUnassigned());
            result.addAll(unit.testDrives().findBySalesman(salesmanId));
            return List.copyOf(result);
        });
    }

    private static TestDrive requireBooking(java.util.Optional<TestDrive> value, long id) {
        return value.orElseThrow(() -> new EntityNotFoundException("test drive", id));
    }

    private static User requireUser(java.util.Optional<User> value, long id) {
        return value.orElseThrow(() -> new EntityNotFoundException("user", id));
    }
}
