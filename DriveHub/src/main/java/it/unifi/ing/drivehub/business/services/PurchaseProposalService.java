package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

public final class PurchaseProposalService {
    private final TransactionRunner transactions;
    private final Clock clock;

    public PurchaseProposalService(DaoFactory daoFactory, Clock clock) {
        this.transactions = new TransactionRunner(daoFactory);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PurchaseProposalService(DaoFactory daoFactory) {
        this(daoFactory, Clock.systemUTC());
    }

    /** UC-C-SELL: registers the customer's vehicle and opens an acquisition request. */
    public PurchaseProposal request(long customerId, long modelId, String plate, long mileage,
                                    BigDecimal requestedAmount, String notes) {
        return transactions.execute(unit -> {
            User customer = unit.users().findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("customer", customerId));
            customer.requireRole(Role.CUSTOMER);
            VehicleModel model = unit.vehicleModels().findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle model", modelId));
            String normalizedPlate = Vehicle.normalizePlate(plate);
            if (unit.vehicles().findByPlate(normalizedPlate).isPresent()) {
                throw new ConflictException("plate is already registered");
            }
            Vehicle acquisition = Vehicle.create(normalizedPlate, model,
                    VehiclePurpose.ACQUISITION_REQUEST, null, null, mileage);
            unit.vehicles().save(acquisition);
            PurchaseProposal proposal = PurchaseProposal.request(customer, acquisition,
                    requestedAmount, notes, Instant.now(clock));
            return unit.purchaseProposals().save(proposal);
        });
    }

    /** UI-friendly UC-C-SELL overload for a vehicle model not yet present in the catalog. */
    public PurchaseProposal request(long customerId, String brandName, String modelName, int modelYear,
                                    String plate, long mileage, BigDecimal requestedAmount, String notes) {
        return transactions.execute(unit -> {
            User customer = unit.users().findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("customer", customerId));
            customer.requireRole(Role.CUSTOMER);
            VehicleModel model = CatalogModels.findOrCreate(unit, brandName, modelName, modelYear);
            String normalizedPlate = Vehicle.normalizePlate(plate);
            if (unit.vehicles().findByPlate(normalizedPlate).isPresent()) {
                throw new ConflictException("plate is already registered");
            }
            Vehicle acquisition = unit.vehicles().save(Vehicle.create(normalizedPlate, model,
                    VehiclePurpose.ACQUISITION_REQUEST, null, null, mileage));
            return unit.purchaseProposals().save(PurchaseProposal.request(customer, acquisition,
                    requestedAmount, notes, Instant.now(clock)));
        });
    }

    /** UC-S-PROPOSAL: first offer wins atomically and binds the salesman. */
    public PurchaseProposal submitOffer(long proposalId, long salesmanId,
                                        BigDecimal offeredAmount, String terms) {
        requirePositive(offeredAmount, "offeredAmount");
        requireText(terms, "terms");
        return transactions.execute(unit -> {
            User salesman = unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId));
            salesman.requireRole(Role.SALESMAN);
            if (!unit.purchaseProposals().submitOfferIfRequested(
                    proposalId, salesmanId, offeredAmount, terms)) {
                throw new ConflictException("proposal was already handled by another salesman");
            }
            return unit.purchaseProposals().findById(proposalId)
                    .orElseThrow(() -> new EntityNotFoundException("purchase proposal", proposalId));
        });
    }

    public PurchaseProposal approve(long proposalId, long managerId, String reason) {
        return decide(proposalId, managerId, reason, true);
    }

    public PurchaseProposal reject(long proposalId, long managerId, String reason) {
        return decide(proposalId, managerId, reason, false);
    }

    /** Requested items plus this salesman's own offers, without exposing colleagues' work. */
    public List<PurchaseProposal> proposalsForSalesman(long salesmanId) {
        return transactions.execute(unit -> {
            unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId)).requireRole(Role.SALESMAN);
            ArrayList<PurchaseProposal> result = new ArrayList<>(unit.purchaseProposals().findRequested());
            result.addAll(unit.purchaseProposals().findBySalesman(salesmanId));
            return List.copyOf(result);
        });
    }

    public List<PurchaseProposal> proposalsForCustomer(long customerId) {
        return transactions.execute(unit -> {
            unit.users().findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("customer", customerId)).requireRole(Role.CUSTOMER);
            return List.copyOf(unit.purchaseProposals().findByCustomer(customerId));
        });
    }

    public List<PurchaseProposal> awaitingManagerDecision(long managerId) {
        return transactions.execute(unit -> {
            unit.users().findById(managerId).orElseThrow(() -> new EntityNotFoundException("manager", managerId))
                    .requireRole(Role.MANAGER);
            return List.copyOf(unit.purchaseProposals().findAwaitingManagerDecision());
        });
    }

    private PurchaseProposal decide(long proposalId, long managerId, String reason, boolean approved) {
        requireText(reason, "reason");
        return transactions.execute(unit -> {
            User manager = unit.users().findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("manager", managerId));
            manager.requireRole(Role.MANAGER);
            if (!unit.purchaseProposals().decideIfOffered(proposalId, managerId, approved, reason, Instant.now(clock))) {
                throw new ConflictException("proposal is no longer awaiting a manager decision");
            }
            return unit.purchaseProposals().findById(proposalId)
                    .orElseThrow(() -> new EntityNotFoundException("purchase proposal", proposalId));
        });
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
