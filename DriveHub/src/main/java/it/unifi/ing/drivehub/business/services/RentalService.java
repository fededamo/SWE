package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.business.strategies.PricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public final class RentalService {
    private final TransactionRunner transactions;
    private final PricingStrategy pricing;

    public RentalService(DaoFactory daoFactory, PricingStrategy pricing) {
        this.transactions = new TransactionRunner(daoFactory);
        this.pricing = Objects.requireNonNull(pricing, "pricing");
    }

    /** UC-C-RENT: creates a customer request with no salesman owner. */
    public Rental requestRental(long customerId, long vehicleId, LocalDate startsOn, LocalDate endsOn) {
        return transactions.execute(unit -> requestRental(unit, customerId, vehicleId, startsOn, endsOn));
    }

    Rental requestRental(UnitOfWork unit, long customerId, long vehicleId,
                         LocalDate startsOn, LocalDate endsOn) {
        Objects.requireNonNull(startsOn, "startsOn");
        Objects.requireNonNull(endsOn, "endsOn");
        if (!endsOn.isAfter(startsOn)) {
            throw new IllegalArgumentException("rental end must be after its start");
        }
        User customer = unit.users().findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("customer", customerId));
        customer.requireRole(Role.CUSTOMER);
        Vehicle vehicle = unit.vehicles().findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
        if (!vehicle.isAvailableForRental()) {
            throw new ConflictException("vehicle is not available for rental");
        }
        if (hasOverlappingRental(unit.rentals().findAll(), vehicleId, startsOn, endsOn)) {
            throw new ConflictException("vehicle already has a rental in the selected period");
        }
        long days = ChronoUnit.DAYS.between(startsOn, endsOn);
        BigDecimal total = pricing.rentalPrice(vehicle, days,
                unit.discounts().findActiveOn(startsOn), startsOn);
        return unit.rentals().save(Rental.request(customer, vehicle, startsOn, endsOn, total));
    }

    /** UC-S-RENT: atomic first-claim wins; ownership is exclusive afterwards. */
    public Rental claimRental(long rentalId, long salesmanId) {
        return transactions.execute(unit -> {
            User salesman = unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId));
            salesman.requireRole(Role.SALESMAN);
            if (!unit.rentals().claimIfUnassigned(rentalId, salesmanId)) {
                throw new ConflictException("rental was already claimed by another salesman");
            }
            return unit.rentals().findById(rentalId)
                    .orElseThrow(() -> new EntityNotFoundException("rental", rentalId));
        });
    }

    public Rental confirmRental(long rentalId, long salesmanId) {
        return transactions.execute(unit -> {
            Rental rental = requireRental(unit.rentals().findByIdForUpdate(rentalId), rentalId);
            User salesman = requireUser(unit.users().findById(salesmanId), "salesman", salesmanId);
            BigDecimal paid = unit.payments().findByReference(PaymentReferenceType.RENTAL, rentalId).stream()
                    .filter(payment -> payment.status() == PaymentStatus.COMPLETED)
                    .map(payment -> payment.amount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (paid.compareTo(rental.totalPrice()) < 0) {
                throw new ConflictException("rental must be paid before confirmation");
            }
            rental.confirm(salesman);
            unit.rentals().update(rental);
            return rental;
        });
    }

    public Rental startRental(long rentalId, long salesmanId) {
        return transactions.execute(unit -> {
            Rental rental = requireRental(unit.rentals().findByIdForUpdate(rentalId), rentalId);
            User salesman = requireUser(unit.users().findById(salesmanId), "salesman", salesmanId);
            rental.start(salesman);
            rental.vehicle().startRental();
            if (!unit.vehicles().updateStatusIfCurrent(rental.vehicle().requireId(),
                    VehicleStatus.AVAILABLE, VehicleStatus.RENTED)) {
                throw new ConflictException("vehicle is no longer available");
            }
            unit.rentals().update(rental);
            return rental;
        });
    }

    public Rental completeRental(long rentalId, long salesmanId) {
        return transactions.execute(unit -> {
            Rental rental = requireRental(unit.rentals().findByIdForUpdate(rentalId), rentalId);
            User salesman = requireUser(unit.users().findById(salesmanId), "salesman", salesmanId);
            rental.complete(salesman);
            rental.vehicle().finishRental();
            if (!unit.vehicles().updateStatusIfCurrent(rental.vehicle().requireId(),
                    VehicleStatus.RENTED, VehicleStatus.AVAILABLE)) {
                throw new ConflictException("vehicle rental state changed concurrently");
            }
            unit.rentals().update(rental);
            return rental;
        });
    }

    public Rental cancelRental(long rentalId, long actorId) {
        return transactions.execute(unit -> {
            Rental rental = requireRental(unit.rentals().findByIdForUpdate(rentalId), rentalId);
            User actor = requireUser(unit.users().findById(actorId), "user", actorId);
            boolean hasPayment = unit.payments().findByReference(PaymentReferenceType.RENTAL, rentalId).stream()
                    .anyMatch(payment -> payment.status() == PaymentStatus.COMPLETED);
            if (hasPayment) {
                throw new ConflictException("Un noleggio pagato non è annullabile: il prototipo non gestisce rimborsi");
            }
            rental.cancel(actor);
            unit.rentals().update(rental);
            return rental;
        });
    }

    public List<Rental> rentalsForCustomer(long customerId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(customerId), "customer", customerId).requireRole(Role.CUSTOMER);
            return List.copyOf(unit.rentals().findByCustomer(customerId));
        });
    }

    public List<Rental> rentalsForSalesman(long salesmanId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(salesmanId), "salesman", salesmanId).requireRole(Role.SALESMAN);
            return List.copyOf(unit.rentals().findBySalesman(salesmanId));
        });
    }

    public List<Rental> unassignedRentals(long salesmanId) {
        return transactions.execute(unit -> {
            requireUser(unit.users().findById(salesmanId), "salesman", salesmanId).requireRole(Role.SALESMAN);
            return List.copyOf(unit.rentals().findUnassigned());
        });
    }

    private static boolean hasOverlappingRental(List<Rental> rentals, long vehicleId,
                                                LocalDate start, LocalDate end) {
        return rentals.stream()
                .filter(rental -> rental.status() != it.unifi.ing.drivehub.domain.rentals.RentalStatus.CANCELLED)
                .filter(rental -> rental.vehicle().id() != null && rental.vehicle().id() == vehicleId)
                .anyMatch(rental -> rental.startsOn().isBefore(end) && start.isBefore(rental.endsOn()));
    }

    private static Rental requireRental(java.util.Optional<Rental> rental, long id) {
        return rental.orElseThrow(() -> new EntityNotFoundException("rental", id));
    }

    private static User requireUser(java.util.Optional<User> user, String type, long id) {
        return user.orElseThrow(() -> new EntityNotFoundException(type, id));
    }
}
