package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DashboardService {
    private static final int RECENT_LIMIT = 10;
    private final TransactionRunner transactions;
    private final InventoryActivityFeed inventoryActivity;

    public DashboardService(DaoFactory daoFactory) {
        this(daoFactory, new InventoryActivityFeed());
    }

    public DashboardService(DaoFactory daoFactory, InventoryActivityFeed inventoryActivity) {
        this.transactions = new TransactionRunner(daoFactory);
        this.inventoryActivity = java.util.Objects.requireNonNull(inventoryActivity);
    }

    public DashboardSnapshot snapshot(long managerId) {
        return transactions.execute(unit -> {
            User manager = unit.users().findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("manager", managerId));
            manager.requireRole(Role.MANAGER);

            List<Vehicle> vehicles = unit.vehicles().findAll();
            List<Rental> rentals = unit.rentals().findAll();
            List<Payment> payments = unit.payments().findAll();
            List<PurchaseProposal> awaiting = unit.purchaseProposals().findAwaitingManagerDecision();

            List<Payment> completed = payments.stream()
                    .filter(payment -> payment.status() == PaymentStatus.COMPLETED)
                    .toList();
            BigDecimal revenue = completed.stream().map(Payment::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ArrayList<DashboardActivity> activity = new ArrayList<>(inventoryActivity.recentActivity());
            completed.forEach(payment -> activity.add(new DashboardActivity(payment.createdAt(),
                    "Pagamento " + (payment.referenceType() == it.unifi.ing.drivehub.domain.sales.PaymentReferenceType.RENTAL ? "noleggio" : "vendita")
                            + " #" + payment.referenceId() + " completato", payment.amount())));
            awaiting.forEach(proposal -> activity.add(new DashboardActivity(proposal.requestedAt(),
                    "Proposta #" + proposal.id() + " in attesa di decisione")));
            List<DashboardActivity> recent = activity.stream()
                    .sorted(Comparator.comparing(DashboardActivity::occurredAt).reversed())
                    .limit(RECENT_LIMIT)
                    .toList();

            return new DashboardSnapshot(
                    vehicles.size(),
                    count(vehicles, VehicleStatus.AVAILABLE, true, false),
                    count(vehicles, VehicleStatus.AVAILABLE, false, true),
                    countStatus(vehicles, VehicleStatus.RENTED),
                    countStatus(vehicles, VehicleStatus.RESERVED),
                    countStatus(vehicles, VehicleStatus.SOLD),
                    countStatus(vehicles, VehicleStatus.MAINTENANCE),
                    rentals.stream().filter(DashboardService::isOpen).count(),
                    awaiting.size(),
                    completed.size(),
                    revenue,
                    recent
            );
        });
    }

    private static long count(List<Vehicle> vehicles, VehicleStatus status, boolean sale, boolean rental) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.status() == status)
                .filter(vehicle -> !sale || vehicle.purpose().supportsSale())
                .filter(vehicle -> !rental || vehicle.purpose().supportsRental())
                .count();
    }

    private static long countStatus(List<Vehicle> vehicles, VehicleStatus status) {
        return vehicles.stream().filter(vehicle -> vehicle.status() == status).count();
    }

    private static boolean isOpen(Rental rental) {
        return rental.status() != RentalStatus.COMPLETED && rental.status() != RentalStatus.CANCELLED;
    }
}
