package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Commits the customer operation and its simulated payment together. The gateway has no
 * external financial side effects: a JDBC rollback is therefore sufficient after an error.
 * A declined charge retains a FAILED audit and a CANCELLED operation in the same commit.
 */
public final class CheckoutService {
    private final TransactionRunner transactions;
    private final RentalService rentals;
    private final SalesService sales;
    private final PaymentService payments;

    public CheckoutService(DaoFactory daoFactory, RentalService rentals,
                           SalesService sales, PaymentService payments) {
        this.transactions = new TransactionRunner(daoFactory);
        this.rentals = Objects.requireNonNull(rentals, "rentals");
        this.sales = Objects.requireNonNull(sales, "sales");
        this.payments = Objects.requireNonNull(payments, "payments");
    }

    public Payment checkoutRental(long customerId, long vehicleId, LocalDate start, LocalDate end,
                                  PaymentMethod method, BigDecimal expectedAmount) {
        Objects.requireNonNull(method, "method");
        return transactions.execute(unit -> {
            Rental rental = rentals.requestRental(unit, customerId, vehicleId, start, end);
            requireAcceptedQuote(expectedAmount, rental.totalPrice());
            Payment payment = payments.payRental(unit, customerId, rental.requireId(), method);
            if (payment.status() == PaymentStatus.FAILED) {
                rental.cancel(rental.customer());
                unit.rentals().update(rental);
            }
            return payment;
        });
    }

    public Payment checkoutSale(long customerId, long vehicleId, boolean fullPurchase,
                                PaymentMethod method, BigDecimal expectedAmount, LocalDate pricingDate) {
        Objects.requireNonNull(method, "method");
        return transactions.execute(unit -> {
            SaleOrder order = sales.reserveVehicle(unit, customerId, vehicleId, null, pricingDate);
            BigDecimal amount = fullPurchase ? order.totalPrice() : order.requiredDeposit();
            requireAcceptedQuote(expectedAmount, amount);
            PaymentPurpose purpose = fullPurchase
                    ? PaymentPurpose.SALE_BALANCE
                    : PaymentPurpose.SALE_DEPOSIT;
            Payment payment = payments.paySaleOrder(unit, customerId, order.requireId(),
                    purpose, method, amount);
            if (payment.status() == PaymentStatus.FAILED) {
                order.cancel(order.customer());
                if (!unit.vehicles().updateStatusIfCurrent(vehicleId,
                        VehicleStatus.RESERVED, VehicleStatus.AVAILABLE)) {
                    throw new ConflictException("vehicle sale state changed concurrently");
                }
                unit.saleOrders().update(order);
            }
            return payment;
        });
    }

    private static void requireAcceptedQuote(BigDecimal expected, BigDecimal actual) {
        Objects.requireNonNull(expected, "expectedAmount");
        if (expected.compareTo(actual) != 0) {
            throw new ConflictException("Il preventivo è cambiato: verificare il nuovo importo prima di pagare");
        }
    }
}
