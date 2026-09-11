package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Customer vehicle-acquisition request, followed by a salesman offer and manager decision. */
public final class PurchaseProposal extends BaseEntity {
    private final User customer;
    private final Vehicle vehicle;
    private final BigDecimal requestedAmount;
    private final String customerNotes;
    private final Instant requestedAt;
    private User salesman;
    private BigDecimal offeredAmount;
    private String offerTerms;
    private User manager;
    private String decisionReason;
    private Instant decidedAt;
    private PurchaseProposalStatus status;

    public PurchaseProposal(Long id, User customer, Vehicle vehicle, BigDecimal requestedAmount,
                            String customerNotes, Instant requestedAt, User salesman,
                            BigDecimal offeredAmount, String offerTerms, User manager,
                            String decisionReason, PurchaseProposalStatus status) {
        this(id, customer, vehicle, requestedAmount, customerNotes, requestedAt, salesman,
                offeredAmount, offerTerms, manager, decisionReason, status, null);
    }

    public PurchaseProposal(Long id, User customer, Vehicle vehicle, BigDecimal requestedAmount,
                            String customerNotes, Instant requestedAt, User salesman,
                            BigDecimal offeredAmount, String offerTerms, User manager,
                            String decisionReason, PurchaseProposalStatus status, Instant decidedAt) {
        super(id);
        this.customer = Objects.requireNonNull(customer, "customer");
        requireStoredRole(customer, Role.CUSTOMER, "customer");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        if (vehicle.purpose() != VehiclePurpose.ACQUISITION_REQUEST) {
            throw new IllegalArgumentException("proposal vehicle must be an acquisition request");
        }
        this.requestedAmount = positive(requestedAmount, "requestedAmount");
        this.customerNotes = requireText(customerNotes, "customerNotes");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.salesman = salesman;
        this.offeredAmount = offeredAmount;
        this.offerTerms = offerTerms;
        this.manager = manager;
        this.decisionReason = decisionReason;
        this.decidedAt = decidedAt;
        this.status = Objects.requireNonNull(status, "status");
        validateState();
    }

    public static PurchaseProposal request(User customer, Vehicle vehicle, BigDecimal requestedAmount,
                                           String customerNotes, Instant requestedAt) {
        return new PurchaseProposal(null, customer, vehicle, requestedAmount, customerNotes, requestedAt,
                null, null, null, null, null, PurchaseProposalStatus.REQUESTED);
    }

    public User customer() { return customer; }
    public Vehicle vehicle() { return vehicle; }
    public BigDecimal requestedAmount() { return requestedAmount; }
    public String customerNotes() { return customerNotes; }
    public Instant requestedAt() { return requestedAt; }
    public User salesman() { return salesman; }
    public BigDecimal offeredAmount() { return offeredAmount; }
    public String offerTerms() { return offerTerms; }
    public User manager() { return manager; }
    public String decisionReason() { return decisionReason; }
    /** Null means no decision or a legacy decision whose instant was not recorded. */
    public Instant decidedAt() { return decidedAt; }
    public PurchaseProposalStatus status() { return status; }

    public void submitOffer(User candidate, BigDecimal amount, String terms) {
        candidate.requireRole(Role.SALESMAN);
        if (status != PurchaseProposalStatus.REQUESTED || salesman != null) {
            throw new DomainRuleViolationException("proposal has already been handled by a salesman");
        }
        BigDecimal validatedAmount = positive(amount, "offeredAmount");
        String validatedTerms = requireText(terms, "offerTerms");
        salesman = candidate;
        offeredAmount = validatedAmount;
        offerTerms = validatedTerms;
        status = PurchaseProposalStatus.OFFERED;
    }

    public void approve(User decisionMaker, String reason) {
        approve(decisionMaker, reason, Instant.now());
    }

    public void reject(User decisionMaker, String reason) {
        reject(decisionMaker, reason, Instant.now());
    }

    public void approve(User decisionMaker, String reason, Instant decisionTime) {
        decide(decisionMaker, reason, PurchaseProposalStatus.APPROVED, decisionTime);
    }

    public void reject(User decisionMaker, String reason, Instant decisionTime) {
        decide(decisionMaker, reason, PurchaseProposalStatus.REJECTED, decisionTime);
    }

    private void decide(User decisionMaker, String reason, PurchaseProposalStatus decision, Instant decisionTime) {
        decisionMaker.requireRole(Role.MANAGER);
        if (status != PurchaseProposalStatus.OFFERED) {
            throw new DomainRuleViolationException("manager can decide only an offered proposal");
        }
        String validatedReason = requireText(reason, "decisionReason");
        Objects.requireNonNull(decisionTime, "decisionTime");
        if (decisionTime.isBefore(requestedAt)) {
            throw new IllegalArgumentException("decision cannot precede the proposal request");
        }
        manager = decisionMaker;
        decisionReason = validatedReason;
        decidedAt = decisionTime;
        status = decision;
    }

    private void validateState() {
        if (decidedAt != null && (decidedAt.isBefore(requestedAt)
                || (status != PurchaseProposalStatus.APPROVED && status != PurchaseProposalStatus.REJECTED))) {
            throw new IllegalArgumentException("decision instant requires a terminal proposal and follows the request");
        }
        if (status == PurchaseProposalStatus.REQUESTED) {
            if (salesman != null || offeredAmount != null || offerTerms != null || manager != null || decisionReason != null) {
                throw new IllegalArgumentException("requested proposal cannot contain offer or decision data");
            }
            return;
        }
        if (salesman == null || offeredAmount == null || offerTerms == null) {
            throw new IllegalArgumentException("offered proposal requires salesman, amount and terms");
        }
        requireStoredRole(salesman, Role.SALESMAN, "salesman");
        positive(offeredAmount, "offeredAmount");
        requireText(offerTerms, "offerTerms");
        if ((status == PurchaseProposalStatus.APPROVED || status == PurchaseProposalStatus.REJECTED)) {
            if (manager == null || decisionReason == null || decisionReason.isBlank()) {
                throw new IllegalArgumentException("decided proposal requires manager and reason");
            }
            requireStoredRole(manager, Role.MANAGER, "manager");
        } else if (manager != null || decisionReason != null) {
            throw new IllegalArgumentException("offered proposal cannot contain a manager decision");
        }
    }

    private static BigDecimal positive(BigDecimal amount, String field) {
        Objects.requireNonNull(amount, field);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return amount;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static void requireStoredRole(User user, Role role, String field) {
        if (!user.hasRole(role)) {
            throw new IllegalArgumentException(field + " must have role " + role);
        }
    }
}
