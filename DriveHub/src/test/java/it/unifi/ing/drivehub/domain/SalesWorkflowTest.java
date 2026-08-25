package it.unifi.ing.drivehub.domain;

import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposalStatus;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.StockOrderStatus;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SalesWorkflowTest {
    @Test
    @DisplayName("UC-C-SELL-01: request, salesman offer and manager approval are ordered")
    void purchaseProposalWorkflow() {
        PurchaseProposal proposal = PurchaseProposal.request(TestFixtures.customer(1L),
                TestFixtures.acquisitionVehicle(40L, "ZA123YX"), new BigDecimal("15000"),
                "Well maintained", Instant.parse("2026-01-01T10:00:00Z"));

        assertThrows(DomainRuleViolationException.class,
                () -> proposal.approve(TestFixtures.manager(3L), "premature"));
        proposal.submitOffer(TestFixtures.salesman(2L), new BigDecimal("13500"), "Subject to inspection");
        proposal.approve(TestFixtures.manager(3L), "Margin approved");

        assertEquals(PurchaseProposalStatus.APPROVED, proposal.status());
        assertEquals(2L, proposal.salesman().id());
        assertEquals(3L, proposal.manager().id());
    }

    @Test
    @DisplayName("UC-C-PURCHASE-01: acconto and balance drive sale order to paid")
    void depositAndBalance() {
        Vehicle vehicle = TestFixtures.saleVehicle(30L, "AB123CD");
        SaleOrder order = SaleOrder.reserve(TestFixtures.customer(1L), vehicle,
                new BigDecimal("40000"), new BigDecimal("4000"));

        order.recordPayment(new BigDecimal("4000"));
        assertEquals(SaleOrderStatus.DEPOSIT_PAID, order.status());
        order.recordPayment(new BigDecimal("36000"));
        assertEquals(SaleOrderStatus.PAID, order.status());

        User salesman = TestFixtures.salesman(2L);
        order.assignSalesman(salesman);
        order.complete(salesman);
        assertEquals(SaleOrderStatus.COMPLETED, order.status());
        assertEquals(VehicleStatus.SOLD, vehicle.status());
    }

    @Test
    @DisplayName("PAY-STATE-01: payment terminal outcome can be recorded only once")
    void paymentHasSingleTerminalOutcome() {
        Payment payment = Payment.start(TestFixtures.customer(1L), PaymentReferenceType.RENTAL, 12L,
                PaymentPurpose.RENTAL, PaymentMethod.CARD, new BigDecimal("250"), Instant.now());

        payment.complete("gateway-123");

        assertEquals(PaymentStatus.COMPLETED, payment.status());
        assertThrows(DomainRuleViolationException.class, () -> payment.fail("late failure"));
    }

    @Test
    @DisplayName("UC-M-STOCK-01: manager confirms and receives a stock order in sequence")
    void stockOrderWorkflow() {
        User manager = TestFixtures.manager(3L);
        StockOrder order = StockOrder.place(manager, TestFixtures.model(), 3,
                new BigDecimal("25000"), LocalDate.of(2026, 8, 1));

        order.confirm(manager);
        order.receive(manager, LocalDate.of(2026, 8, 20));

        assertEquals(StockOrderStatus.RECEIVED, order.status());
        assertEquals(new BigDecimal("75000"), order.totalCost());
    }
}
