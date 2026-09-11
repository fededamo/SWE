package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.PaymentGateway;
import it.unifi.ing.drivehub.business.core.PaymentGatewayResult;
import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class PaymentService {
    private final TransactionRunner transactions;
    private final PaymentGateway gateway;
    private final Clock clock;

    public PaymentService(DaoFactory daoFactory, PaymentGateway gateway, Clock clock) {
        this.transactions = new TransactionRunner(daoFactory);
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PaymentService(DaoFactory daoFactory, PaymentGateway gateway) {
        this(daoFactory, gateway, Clock.systemUTC());
    }

    /** Included UC payment for rental: one approved payment covers the outstanding rental total. */
    public Payment payRental(long customerId, long rentalId, PaymentMethod method) {
        return transactions.execute(unit -> payRental(unit, customerId, rentalId, method));
    }

    Payment payRental(UnitOfWork unit, long customerId, long rentalId, PaymentMethod method) {
        User customer = requireCustomer(unit.users().findById(customerId), customerId);
        Rental rental = unit.rentals().findByIdForUpdate(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("rental", rentalId));
        requireOwner(customer, rental.customer());
        if (rental.status() != RentalStatus.REQUESTED && rental.status() != RentalStatus.ASSIGNED) {
            throw new ConflictException("rental does not accept payment in status " + rental.status());
        }
        BigDecimal alreadyPaid = completedTotal(
                unit.payments().findByReference(PaymentReferenceType.RENTAL, rentalId));
        BigDecimal outstanding = rental.totalPrice().subtract(alreadyPaid);
        if (outstanding.signum() <= 0) {
            throw new ConflictException("rental is already paid");
        }
        Payment payment = Payment.start(customer, PaymentReferenceType.RENTAL, rentalId,
                PaymentPurpose.RENTAL, method, outstanding, Instant.now(clock));
        unit.payments().save(payment);
        settle(payment);
        unit.payments().update(payment);
        return payment;
    }

    /** Included UC payment for a sale deposit or remaining balance. */
    public Payment paySaleOrder(long customerId, long orderId, PaymentPurpose purpose,
                                PaymentMethod method, BigDecimal amount) {
        return transactions.execute(unit -> paySaleOrder(unit, customerId, orderId, purpose, method, amount));
    }

    Payment paySaleOrder(UnitOfWork unit, long customerId, long orderId, PaymentPurpose purpose,
                         PaymentMethod method, BigDecimal amount) {
        if (purpose != PaymentPurpose.SALE_DEPOSIT && purpose != PaymentPurpose.SALE_BALANCE) {
            throw new IllegalArgumentException("sale order requires a sale payment purpose");
        }
        User customer = requireCustomer(unit.users().findById(customerId), customerId);
        SaleOrder order = unit.saleOrders().findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("sale order", orderId));
        requireOwner(customer, order.customer());
        validateSalePayment(order, purpose, amount);
        Payment payment = Payment.start(customer, PaymentReferenceType.SALE_ORDER, orderId,
                purpose, method, amount, Instant.now(clock));
        unit.payments().save(payment);
        settle(payment);
        unit.payments().update(payment);
        if (payment.status() == PaymentStatus.COMPLETED) {
            order.recordPayment(amount);
            unit.saleOrders().update(order);
        }
        return payment;
    }

    public List<Payment> paymentsFor(long customerId, PaymentReferenceType referenceType, long referenceId) {
        Objects.requireNonNull(referenceType, "referenceType");
        return transactions.execute(unit -> {
            User customer = requireCustomer(unit.users().findById(customerId), customerId);
            User owner = referenceType == PaymentReferenceType.RENTAL
                    ? unit.rentals().findById(referenceId).orElseThrow(() -> new EntityNotFoundException("rental", referenceId)).customer()
                    : unit.saleOrders().findById(referenceId).orElseThrow(() -> new EntityNotFoundException("sale order", referenceId)).customer();
            requireOwner(customer, owner);
            return List.copyOf(unit.payments().findByReference(referenceType, referenceId));
        });
    }

    private void settle(Payment payment) {
        PaymentGatewayResult result = Objects.requireNonNull(
                gateway.charge(payment.amount(), payment.method()), "gateway result");
        if (result.approved()) {
            payment.complete(result.processorReference());
        } else {
            payment.fail(result.rejectionReason());
        }
    }

    private static void validateSalePayment(SaleOrder order, PaymentPurpose purpose, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        BigDecimal outstanding = order.totalPrice().subtract(order.paidAmount());
        if (amount.compareTo(outstanding) > 0) {
            throw new ConflictException("payment exceeds outstanding order balance");
        }
        if (purpose == PaymentPurpose.SALE_DEPOSIT) {
            if (order.status() != SaleOrderStatus.RESERVED) {
                throw new ConflictException("deposit is accepted only for a reserved order");
            }
            BigDecimal depositOutstanding = order.requiredDeposit().subtract(order.paidAmount());
            if (amount.compareTo(depositOutstanding) != 0) {
                throw new ConflictException("deposit payment must cover the exact required deposit");
            }
        } else {
            if (order.status() != SaleOrderStatus.RESERVED
                    && order.status() != SaleOrderStatus.DEPOSIT_PAID) {
                throw new ConflictException("balance is accepted only for a reserved or deposit-paid order");
            }
            if (amount.compareTo(outstanding) != 0) {
                throw new ConflictException("balance or full payment must cover the exact outstanding amount");
            }
        }
    }

    private static BigDecimal completedTotal(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> payment.status() == PaymentStatus.COMPLETED)
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static User requireCustomer(java.util.Optional<User> value, long id) {
        User customer = value.orElseThrow(() -> new EntityNotFoundException("customer", id));
        customer.requireRole(Role.CUSTOMER);
        return customer;
    }

    private static void requireOwner(User actor, User owner) {
        boolean same = actor.id() != null && owner.id() != null
                ? actor.id().equals(owner.id()) : actor == owner;
        if (!same) {
            throw new ConflictException("payment can be made only by the owning customer");
        }
    }
}
