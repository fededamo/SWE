package it.unifi.ing.drivehub.domain.sales;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Payment extends BaseEntity {
    private final User payer;
    private final PaymentReferenceType referenceType;
    private final long referenceId;
    private final PaymentPurpose purpose;
    private final PaymentMethod method;
    private final BigDecimal amount;
    private final Instant createdAt;
    private PaymentStatus status;
    private String processorReference;
    private String failureReason;

    public Payment(Long id, User payer, PaymentReferenceType referenceType, long referenceId,
                   PaymentPurpose purpose, PaymentMethod method, BigDecimal amount, Instant createdAt,
                   PaymentStatus status, String processorReference, String failureReason) {
        super(id);
        this.payer = Objects.requireNonNull(payer, "payer");
        if (!payer.hasRole(Role.CUSTOMER)) {
            throw new IllegalArgumentException("payer must have role CUSTOMER");
        }
        this.referenceType = Objects.requireNonNull(referenceType, "referenceType");
        if (referenceId <= 0) {
            throw new IllegalArgumentException("referenceId must be positive");
        }
        this.referenceId = referenceId;
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        validateReferencePurpose(referenceType, purpose);
        this.method = Objects.requireNonNull(method, "method");
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
        this.processorReference = processorReference;
        this.failureReason = failureReason;
    }

    public static Payment start(User payer, PaymentReferenceType referenceType, long referenceId,
                                PaymentPurpose purpose, PaymentMethod method, BigDecimal amount,
                                Instant createdAt) {
        return new Payment(null, payer, referenceType, referenceId, purpose, method, amount, createdAt,
                PaymentStatus.PENDING, null, null);
    }

    public User payer() { return payer; }
    public PaymentReferenceType referenceType() { return referenceType; }
    public long referenceId() { return referenceId; }
    public PaymentPurpose purpose() { return purpose; }
    public PaymentMethod method() { return method; }
    public BigDecimal amount() { return amount; }
    public Instant createdAt() { return createdAt; }
    public PaymentStatus status() { return status; }
    public String processorReference() { return processorReference; }
    public String failureReason() { return failureReason; }

    public void complete(String processorReference) {
        requirePending();
        if (processorReference == null || processorReference.isBlank()) {
            throw new IllegalArgumentException("processorReference must not be blank");
        }
        this.processorReference = processorReference.trim();
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail(String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("failure reason must not be blank");
        }
        this.failureReason = reason.trim();
        this.status = PaymentStatus.FAILED;
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw new DomainRuleViolationException("payment has already reached a terminal state");
        }
    }

    private static void validateReferencePurpose(PaymentReferenceType type, PaymentPurpose purpose) {
        if (type == PaymentReferenceType.RENTAL && purpose != PaymentPurpose.RENTAL) {
            throw new IllegalArgumentException("rental reference requires rental payment purpose");
        }
        if (type == PaymentReferenceType.SALE_ORDER && purpose == PaymentPurpose.RENTAL) {
            throw new IllegalArgumentException("sale order cannot use rental payment purpose");
        }
    }
}
