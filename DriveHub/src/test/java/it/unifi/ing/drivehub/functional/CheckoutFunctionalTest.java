package it.unifi.ing.drivehub.functional;

import it.unifi.ing.drivehub.business.core.PaymentGatewayResult;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.sales.*;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutFunctionalTest extends FunctionalTestSupport {
    @Test
    @DisplayName("FT-RENT-01 / FT-PAY-01: accepted rental checkout commits request and complete payment")
    void ftRent01AcceptedCheckout() {
        Payment payment = checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(), DAY,
                DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL);
        assertEquals(PaymentStatus.COMPLETED, payment.status());
        assertEquals(RentalStatus.REQUESTED, rental(payment.referenceId()).status());
        assertEquals(RENT_TOTAL, payment.amount());
        assertEquals(CLOCK.instant(), payment.createdAt());
        assertEquals(1, payments.paymentsFor(customer.requireId(), PaymentReferenceType.RENTAL, payment.referenceId()).size());
    }

    @Test
    @DisplayName("FT-RENT-03 / FT-PAY-02: decline commits failed audit and cancelled rental together")
    void ftRent03DeclinedCheckout() {
        withGateway((amount, method) -> PaymentGatewayResult.rejected("simulated decline"));
        Payment payment = checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(), DAY,
                DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL);
        assertEquals(PaymentStatus.FAILED, payment.status());
        assertEquals(RentalStatus.CANCELLED, rental(payment.referenceId()).status());
        assertEquals(0, dashboard.snapshot(manager.requireId()).completedPayments());
        assertThrows(ConflictException.class,
                () -> payments.payRental(customer.requireId(), payment.referenceId(), PaymentMethod.CARD));
        // A declined request releases its dates and can be attempted again.
        assertDoesNotThrow(() -> rentals.requestRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(3)));
    }

    @Test
    @DisplayName("FT-RENT-02: invalid and overlapping periods are rejected; adjacent periods are allowed")
    void ftRent02Periods() {
        assertThrows(IllegalArgumentException.class, () -> rentals.requestRental(customer.requireId(),
                rentalVehicle.requireId(), DAY, DAY));
        rentals.requestRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(3));
        assertThrows(ConflictException.class, () -> rentals.requestRental(otherCustomer.requireId(),
                rentalVehicle.requireId(), DAY.plusDays(2), DAY.plusDays(4)));
        assertDoesNotThrow(() -> rentals.requestRental(otherCustomer.requireId(), rentalVehicle.requireId(),
                DAY.plusDays(3), DAY.plusDays(4)));
    }

    @Test
    @DisplayName("FT-RENT-05/06/08: customer views and cancellations preserve ownership and payment rules")
    void ftRent05OwnershipAndCancellation() {
        Rental unpaid = rentals.requestRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(3));
        assertTrue(rentals.rentalsForCustomer(otherCustomer.requireId()).isEmpty());
        assertThrows(DomainRuleViolationException.class,
                () -> rentals.cancelRental(unpaid.requireId(), otherCustomer.requireId()));
        assertEquals(RentalStatus.CANCELLED, rentals.cancelRental(unpaid.requireId(), customer.requireId()).status());
        Payment paid = checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(), DAY,
                DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL);
        assertThrows(ConflictException.class, () -> rentals.cancelRental(paid.referenceId(), customer.requireId()));
        assertEquals(RentalStatus.REQUESTED, rental(paid.referenceId()).status());
    }

    @Test
    @DisplayName("FT-RENT-07/08: paid rental claim, confirm, start, return and critical illegal transitions")
    void ftRent07StaffWorkflow() {
        Payment payment = checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(), DAY,
                DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL);
        long rentalId = payment.referenceId();
        rentals.claimRental(rentalId, salesman.requireId());
        assertThrows(ConflictException.class, () -> rentals.claimRental(rentalId, otherSalesman.requireId()));
        assertThrows(DomainRuleViolationException.class, () -> rentals.confirmRental(rentalId, otherSalesman.requireId()));
        assertThrows(DomainRuleViolationException.class, () -> rentals.startRental(rentalId, salesman.requireId()));
        rentals.confirmRental(rentalId, salesman.requireId());
        rentals.startRental(rentalId, salesman.requireId());
        assertEquals(VehicleStatus.RENTED, vehicle(rentalVehicle.requireId()).status());
        rentals.completeRental(rentalId, salesman.requireId());
        assertEquals(RentalStatus.COMPLETED, rental(rentalId).status());
        assertEquals(VehicleStatus.AVAILABLE, vehicle(rentalVehicle.requireId()).status());
        assertThrows(DomainRuleViolationException.class, () -> rentals.completeRental(rentalId, salesman.requireId()));
    }

    @Test
    @DisplayName("FT-PAY-03: a completed rental cannot be charged again or paid by another customer")
    void ftPay03DuplicateAndNonOwner() {
        Rental unpaid = rentals.requestRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(3));
        assertThrows(ConflictException.class,
                () -> payments.payRental(otherCustomer.requireId(), unpaid.requireId(), PaymentMethod.CARD));
        payments.payRental(customer.requireId(), unpaid.requireId(), PaymentMethod.CARD);
        assertThrows(ConflictException.class,
                () -> payments.payRental(customer.requireId(), unpaid.requireId(), PaymentMethod.CARD));
        assertEquals(1, charges.get());
    }

    @Test
    @DisplayName("FT-SALE-01/02: accepted deposit, exact balance, salesman delivery and second balance refusal")
    void ftSale01DepositBalanceDelivery() {
        SaleOrder invalidDepositOrder = sales.reserveVehicle(customer.requireId(), saleVehicle.requireId(),
                new BigDecimal("4000.00"), DAY);
        assertThrows(ConflictException.class, () -> payments.paySaleOrder(customer.requireId(),
                invalidDepositOrder.requireId(), PaymentPurpose.SALE_DEPOSIT,
                PaymentMethod.CARD, new BigDecimal("5000.00")));
        sales.cancelOrder(invalidDepositOrder.requireId(), customer.requireId());

        Payment deposit = checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(), false,
                PaymentMethod.CARD, new BigDecimal("4000.00"), DAY);
        long orderId = deposit.referenceId();
        assertEquals(PaymentPurpose.SALE_DEPOSIT, deposit.purpose());
        assertEquals(SaleOrderStatus.DEPOSIT_PAID, order(orderId).status());
        assertEquals(VehicleStatus.RESERVED, vehicle(saleVehicle.requireId()).status());
        assertThrows(DomainRuleViolationException.class, () -> sales.cancelOrder(orderId, customer.requireId()));
        assertThrows(ConflictException.class, () -> payments.paySaleOrder(customer.requireId(), orderId,
                PaymentPurpose.SALE_BALANCE, PaymentMethod.CARD, new BigDecimal("35000")));
        payments.paySaleOrder(customer.requireId(), orderId, PaymentPurpose.SALE_BALANCE,
                PaymentMethod.CARD, new BigDecimal("36000.00"));
        assertEquals(SaleOrderStatus.PAID, order(orderId).status());
        assertThrows(ConflictException.class, () -> payments.paySaleOrder(customer.requireId(), orderId,
                PaymentPurpose.SALE_BALANCE, PaymentMethod.CARD, new BigDecimal("36000.00")));
        sales.claimOrder(orderId, salesman.requireId());
        assertThrows(ConflictException.class, () -> sales.claimOrder(orderId, otherSalesman.requireId()));
        assertThrows(DomainRuleViolationException.class, () -> sales.completeOrder(orderId, otherSalesman.requireId()));
        sales.completeOrder(orderId, salesman.requireId());
        assertEquals(SaleOrderStatus.COMPLETED, order(orderId).status());
        assertEquals(VehicleStatus.SOLD, vehicle(saleVehicle.requireId()).status());
        assertThrows(DomainRuleViolationException.class, () -> sales.completeOrder(orderId, salesman.requireId()));
        assertEquals(2, charges.get());
    }

    @Test
    @DisplayName("FT-SALE-03: failed deposit leaves cancelled order and releases the vehicle atomically")
    void ftSale03DeclinedDeposit() {
        withGateway((amount, method) -> PaymentGatewayResult.rejected("simulated decline"));
        Payment payment = checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(), false,
                PaymentMethod.CARD, new BigDecimal("4000.00"), DAY);
        assertEquals(PaymentStatus.FAILED, payment.status());
        assertEquals(SaleOrderStatus.CANCELLED, order(payment.referenceId()).status());
        assertEquals(0, order(payment.referenceId()).paidAmount().signum());
        assertEquals(VehicleStatus.AVAILABLE, vehicle(saleVehicle.requireId()).status());
    }

    @Test
    @DisplayName("FT-SALE-03: full purchase reserves once and refuses a competing customer")
    void ftSale03FullPurchaseOnce() {
        Payment payment = checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(), true,
                PaymentMethod.CARD, new BigDecimal("40000.00"), DAY);
        assertEquals(PaymentPurpose.SALE_BALANCE, payment.purpose());
        assertEquals(SaleOrderStatus.PAID, order(payment.referenceId()).status());
        assertThrows(ConflictException.class, () -> checkout.checkoutSale(otherCustomer.requireId(),
                saleVehicle.requireId(), true, PaymentMethod.CARD, new BigDecimal("40000.00"), DAY));
        assertEquals(1, charges.get());
    }

    @Test
    @DisplayName("FT-PAY-02: failed balance preserves the previously committed deposit and permits retry")
    void ftPay02FailedBalanceThenRetry() {
        Payment deposit = checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(), false,
                PaymentMethod.CARD, new BigDecimal("4000.00"), DAY);
        withGateway((amount, method) -> PaymentGatewayResult.rejected("simulated decline"));
        Payment failed = payments.paySaleOrder(customer.requireId(), deposit.referenceId(), PaymentPurpose.SALE_BALANCE,
                PaymentMethod.CARD, new BigDecimal("36000.00"));
        assertEquals(PaymentStatus.FAILED, failed.status());
        assertEquals(new BigDecimal("4000.00"), order(deposit.referenceId()).paidAmount());
        assertEquals(SaleOrderStatus.DEPOSIT_PAID, order(deposit.referenceId()).status());
        withGateway((amount, method) -> PaymentGatewayResult.approved("retry-success"));
        payments.paySaleOrder(customer.requireId(), deposit.referenceId(), PaymentPurpose.SALE_BALANCE,
                PaymentMethod.CARD, new BigDecimal("36000.00"));
        assertEquals(SaleOrderStatus.PAID, order(deposit.referenceId()).status());
    }

    @Test
    @DisplayName("FT-PAY-04: stale accepted quote is rejected before calling the gateway")
    void ftPay04StaleQuote() {
        pricing.updateDailyRentalRate(manager.requireId(), rentalVehicle.requireId(), new BigDecimal("120.00"));
        assertThrows(ConflictException.class, () -> checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(),
                DAY, DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL));
        assertTrue(rentals.rentalsForCustomer(customer.requireId()).isEmpty());
        assertEquals(0, charges.get());
        assertThrows(ConflictException.class, () -> checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(),
                false, PaymentMethod.CARD, new BigDecimal("3900.00"), DAY));
        assertTrue(sales.ordersForCustomer(customer.requireId()).isEmpty());
        assertEquals(VehicleStatus.AVAILABLE, vehicle(saleVehicle.requireId()).status());
    }

    @Test
    @DisplayName("FT-PAY-05: unexpected simulated gateway error rolls back request, payment and reservation")
    void ftPay05GatewayExceptionRollback() {
        withGateway((amount, method) -> { throw new IllegalStateException("injected gateway failure"); });
        assertThrows(IllegalStateException.class, () -> checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(),
                DAY, DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL));
        assertTrue(rentals.rentalsForCustomer(customer.requireId()).isEmpty());
        assertThrows(IllegalStateException.class, () -> checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(),
                true, PaymentMethod.CARD, new BigDecimal("40000.00"), DAY));
        assertTrue(sales.ordersForCustomer(customer.requireId()).isEmpty());
        assertEquals(VehicleStatus.AVAILABLE, vehicle(saleVehicle.requireId()).status());
        assertEquals(0, transactions.execute(unit -> unit.payments().findAll().size()).intValue());
    }
}
