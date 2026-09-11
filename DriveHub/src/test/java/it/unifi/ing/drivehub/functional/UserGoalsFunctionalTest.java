package it.unifi.ing.drivehub.functional;

import it.unifi.ing.drivehub.business.exceptions.*;
import it.unifi.ing.drivehub.business.services.*;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.rentals.TestDriveStatus;
import it.unifi.ing.drivehub.domain.sales.*;
import it.unifi.ing.drivehub.domain.users.*;
import it.unifi.ing.drivehub.domain.vehicles.*;
import it.unifi.ing.drivehub.presentation.core.ServiceUiGateway;
import it.unifi.ing.drivehub.presentation.core.SessionContext;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class UserGoalsFunctionalTest extends FunctionalTestSupport {
    @ParameterizedTest @EnumSource(Role.class)
    @DisplayName("FT-AUTH-01/03: registration and login for every persisted role")
    void ftAuth01RegisterAndLogin(Role role) {
        User user = register(20 + role.ordinal(), role);
        User logged = auth.login(user.email().toUpperCase(), "test-pass-123".toCharArray());
        assertEquals(user.id(), logged.id());
        assertEquals(role, logged.role());
        assertNotEquals("test-pass-123", logged.passwordHash());
    }

    @Test @DisplayName("FT-AUTH-02: duplicate identity and invalid registration never create an account")
    void ftAuth02DuplicatesAndInvalidData() {
        assertThrows(ConflictException.class, () -> register(1, Role.CUSTOMER));
        assertThrows(ConflictException.class, () -> auth.register(new RegistrationRequest(
                "TESTUSER00000021", "Demo", "User", customer.email(), "+390550000000", Role.MANAGER, null),
                "test-pass-123".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.register(new RegistrationRequest(
                "TESTUSER00000021", "Demo", "User", "invalid", "+390550000000", Role.CUSTOMER, null),
                "test-pass-123".toCharArray()));
        assertEquals(2, transactions.execute(u -> u.users().findByRole(Role.CUSTOMER).size()).intValue());
    }

    @Test @DisplayName("FT-AUTH-04: wrong password, absent user and inactive account deny login identically")
    void ftAuth04LoginDenials() {
        String message = assertThrows(AuthenticationException.class,
                () -> auth.login(customer.email(), "wrong-pass".toCharArray())).getMessage();
        assertEquals(message, assertThrows(AuthenticationException.class,
                () -> auth.login("absent@example.test", "wrong-pass".toCharArray())).getMessage());
        transactions.execute(u -> { customer.deactivate(); u.users().update(customer); return null; });
        assertEquals(message, assertThrows(AuthenticationException.class,
                () -> auth.login(customer.email(), "test-pass-123".toCharArray())).getMessage());
    }

    @Test @DisplayName("FT-AUTH-05: logout cleanup is idempotent and denies subsequent authenticated access")
    void ftAuth05SessionCleanup() {
        SessionContext session = new SessionContext();
        session.open(new UiModels.Session(customer.requireId(), customer.displayName(), customer.email(), customer.role()));
        session.clear(); session.clear();
        assertThrows(IllegalStateException.class, session::requireCurrent);
    }

    @Test @DisplayName("FT-CAT-01/02: combined catalog filters, empty result and negative price")
    void ftCat01CombinedFilters() {
        ServiceUiGateway ui = new ServiceUiGateway(auth, new CatalogService(factory), rentals, testDrives,
                proposals, pricing, inventory, dashboard, checkout);
        assertEquals(1, ui.searchCatalog(" example ", VehiclePurpose.RENTAL, new BigDecimal("100")).size());
        assertTrue(ui.searchCatalog("absent", null, null).isEmpty());
        assertTrue(ui.searchCatalog("", VehiclePurpose.FOR_SALE, new BigDecimal("39999")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ui.searchCatalog("", null, BigDecimal.ONE.negate()));
    }

    @Test @DisplayName("FT-TD-01/02: free slot, customer and vehicle overlap, adjacency and past date")
    void ftTd01Slots() {
        var start = DAY.atTime(10, 0);
        var booking = testDrives.book(customer.requireId(), saleVehicle.requireId(), start, Duration.ofMinutes(45));
        assertEquals(TestDriveStatus.REQUESTED, booking.status());
        assertThrows(ConflictException.class, () -> testDrives.book(otherCustomer.requireId(), saleVehicle.requireId(), start.plusMinutes(20), Duration.ofMinutes(45)));
        Vehicle second = inventory.addVehicle(salesman.requireId(), model.requireId(), "TD200AA", VehiclePurpose.FOR_SALE, new BigDecimal("9000"), null, 0);
        assertThrows(ConflictException.class, () -> testDrives.book(customer.requireId(), second.requireId(), start.plusMinutes(20), Duration.ofMinutes(45)));
        assertDoesNotThrow(() -> testDrives.book(customer.requireId(), second.requireId(), start.plusMinutes(45), Duration.ofMinutes(45)));
        assertThrows(IllegalArgumentException.class, () -> testDrives.book(otherCustomer.requireId(), second.requireId(), LocalDateTime.now(CLOCK).minusMinutes(1), Duration.ofMinutes(45)));
    }

    @Test @DisplayName("FT-TD-03/04: staff lifecycle, competing claim, ownership and repeated completion")
    void ftTd03WorkflowAndOwnership() {
        var booking = testDrives.book(customer.requireId(), saleVehicle.requireId(), DAY.atTime(10, 0), Duration.ofMinutes(45));
        long id = booking.requireId();
        testDrives.confirm(id, salesman.requireId());
        assertThrows(ConflictException.class, () -> testDrives.confirm(id, otherSalesman.requireId()));
        assertThrows(DomainRuleViolationException.class, () -> testDrives.start(id, otherSalesman.requireId()));
        assertThrows(DomainRuleViolationException.class, () -> testDrives.complete(id, salesman.requireId()));
        testDrives.start(id, salesman.requireId());
        assertEquals(VehicleStatus.TEST_DRIVE, vehicle(saleVehicle.requireId()).status());
        testDrives.complete(id, salesman.requireId());
        assertEquals(VehicleStatus.AVAILABLE, vehicle(saleVehicle.requireId()).status());
        assertThrows(DomainRuleViolationException.class, () -> testDrives.complete(id, salesman.requireId()));
    }

    @Test @DisplayName("FT-TD-04: cancellation checks ownership and inactive accounts")
    void ftTd04Cancellation() {
        var booking = testDrives.book(customer.requireId(), saleVehicle.requireId(), DAY.atTime(10, 0), Duration.ofMinutes(45));
        assertThrows(DomainRuleViolationException.class, () -> testDrives.cancel(booking.requireId(), otherCustomer.requireId()));
        transactions.execute(u -> { customer.deactivate(); u.users().update(customer); return null; });
        assertThrows(DomainRuleViolationException.class, () -> testDrives.cancel(booking.requireId(), customer.requireId()));
        testDrives.confirm(booking.requireId(), salesman.requireId());
        assertEquals(TestDriveStatus.CANCELLED, testDrives.cancel(booking.requireId(), salesman.requireId()).status());
    }

    @Test @DisplayName("FT-ACQ-01/02: acquisition request and duplicate plate rollback including newly created catalog")
    void ftAcq01RequestAndRollback() {
        var proposal = proposals.request(customer.requireId(), "New Demo", "Model", 2026, "AC100AA", 10, BigDecimal.TEN, "Demo request");
        assertEquals(PurchaseProposalStatus.REQUESTED, proposal.status());
        assertEquals(VehiclePurpose.ACQUISITION_REQUEST, proposal.vehicle().purpose());
        assertThrows(ConflictException.class, () -> proposals.request(customer.requireId(), "Rollback Brand", "Rollback Model", 2026, "AC100AA", 10, BigDecimal.TEN, "Duplicate"));
        assertEquals(Boolean.TRUE, transactions.execute(u -> u.brands().findByName("Rollback Brand").isEmpty()));
        assertThrows(IllegalArgumentException.class, () -> proposals.request(customer.requireId(), "Rollback Brand", "Model", 2026, "AC200AA", -1, BigDecimal.TEN, "Invalid"));
        assertEquals(1, proposals.proposalsForCustomer(customer.requireId()).size());
        assertTrue(proposals.proposalsForCustomer(otherCustomer.requireId()).isEmpty());
    }

    @Test @DisplayName("FT-ACQ-03/04/05/06: offer then approval or refusal, with decision instant and no second decision")
    void ftAcq03OfferAndBothDecisions() {
        for (boolean approve : new boolean[]{true, false}) {
            var p = proposals.request(customer.requireId(), model.requireId(), approve ? "AC300AA" : "AC400AA", 0, BigDecimal.TEN, "Demo");
            proposals.submitOffer(p.requireId(), salesman.requireId(), BigDecimal.TEN, "Terms");
            assertThrows(ConflictException.class, () -> proposals.submitOffer(p.requireId(), otherSalesman.requireId(), BigDecimal.TEN, "Repeated"));
            assertTrue(proposals.proposalsForSalesman(otherSalesman.requireId()).isEmpty());
            var decided = approve ? proposals.approve(p.requireId(), manager.requireId(), "Approved")
                    : proposals.reject(p.requireId(), manager.requireId(), "Rejected");
            assertEquals(approve ? PurchaseProposalStatus.APPROVED : PurchaseProposalStatus.REJECTED, decided.status());
            assertEquals(CLOCK.instant(), decided.decidedAt());
            assertThrows(ConflictException.class, () -> proposals.reject(p.requireId(), manager.requireId(), "Repeated"));
        }
        assertTrue(proposals.awaitingManagerDecision(manager.requireId()).isEmpty());
    }

    @Test @DisplayName("FT-INV-01: committed inventory notifications feed dashboard activity and unsubscribe works")
    void ftInv01CommittedObserver() {
        InventoryActivityFeed feed = new InventoryActivityFeed(); inventory.addObserver(feed);
        DashboardService observedDashboard = new DashboardService(factory, feed);
        inventory.sendToMaintenance(salesman.requireId(), rentalVehicle.requireId());
        assertEquals(VehicleStatus.MAINTENANCE, vehicle(rentalVehicle.requireId()).status());
        assertTrue(observedDashboard.snapshot(manager.requireId()).recentActivity().getFirst().description().contains("MAINTENANCE"));
        inventory.removeObserver(feed);
        inventory.returnFromMaintenance(salesman.requireId(), rentalVehicle.requireId());
        assertEquals(1, feed.recentActivity().size());
    }

    @Test @DisplayName("FT-INV-02: future bookings prohibit maintenance and sale without notifying observers")
    void ftInv02CommittedBookingsBlockAvailabilityChanges() {
        InventoryActivityFeed feed = new InventoryActivityFeed(); inventory.addObserver(feed);
        rentals.requestRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(1));
        assertThrows(ConflictException.class, () -> inventory.sendToMaintenance(salesman.requireId(), rentalVehicle.requireId()));
        testDrives.book(customer.requireId(), saleVehicle.requireId(), DAY.atTime(10, 0), Duration.ofMinutes(45));
        assertThrows(ConflictException.class, () -> inventory.sendToMaintenance(salesman.requireId(), saleVehicle.requireId()));
        assertThrows(ConflictException.class, () -> checkout.checkoutSale(customer.requireId(), saleVehicle.requireId(), true, PaymentMethod.CARD, new BigDecimal("40000"), DAY));
        assertTrue(feed.recentActivity().isEmpty());
        assertEquals(0, charges.get());
    }

    @Test @DisplayName("FT-DASH-01/02: aggregates from known data, no invented activity and wrong role denied")
    void ftDash01KnownAggregates() {
        assertTrue(dashboard.snapshot(manager.requireId()).recentActivity().isEmpty());
        checkout.checkoutRental(customer.requireId(), rentalVehicle.requireId(), DAY, DAY.plusDays(3), PaymentMethod.CARD, RENT_TOTAL);
        var snapshot = dashboard.snapshot(manager.requireId());
        assertEquals(2, snapshot.totalVehicles()); assertEquals(1, snapshot.openRentals());
        assertEquals(RENT_TOTAL, snapshot.recentActivity().getFirst().amount());
        assertEquals(RENT_TOTAL, snapshot.recordedRevenue()); assertEquals(1, snapshot.completedPayments());
        assertThrows(DomainRuleViolationException.class, () -> dashboard.snapshot(customer.requireId()));
    }

    @Test @DisplayName("FT-STOCK-01/02: create missing model atomically, reuse identity, and roll back invalid order")
    void ftStock01CreationAndRollback() {
        var first = inventory.placeStockOrder(manager.requireId(), "New Stock", "Model", 2026, 2, new BigDecimal("9000"));
        assertEquals(StockOrderStatus.PLACED, first.status());
        var second = inventory.placeStockOrder(manager.requireId(), "New Stock", "Model", 2026, 1, new BigDecimal("9000"));
        assertEquals(first.model().id(), second.model().id());
        assertEquals(new BigDecimal("18000"), first.totalCost());
        assertThrows(IllegalArgumentException.class, () -> inventory.placeStockOrder(manager.requireId(), "Invalid Stock", "Model", 2026, 0, BigDecimal.TEN));
        assertEquals(Boolean.TRUE, transactions.execute(u -> u.brands().findByName("Invalid Stock").isEmpty()));
        inventory.confirmStockOrder(manager.requireId(), first.requireId());
        inventory.receiveStockOrder(manager.requireId(), first.requireId());
        assertThrows(DomainRuleViolationException.class, () -> inventory.receiveStockOrder(manager.requireId(), first.requireId()));
    }

    @Test @DisplayName("FT-PRICE-01/02/03: correct price, one replaceable promotion, removal and invalid update rollback")
    void ftPrice01PricingAndPromotion() {
        pricing.updateSalePrice(manager.requireId(), saleVehicle.requireId(), new BigDecimal("30000"));
        var discount = pricing.applyDiscount(manager.requireId(), "Demo", saleVehicle.requireId(), BigDecimal.TEN, DAY, DAY.plusDays(1));
        assertEquals(new BigDecimal("27000.00"), pricing.quoteSale(saleVehicle.requireId(), DAY));
        assertThrows(IllegalArgumentException.class, () -> pricing.applyDiscount(manager.requireId(), "Bad", saleVehicle.requireId(), new BigDecimal("100"), DAY, DAY));
        assertThrows(IllegalArgumentException.class, () -> pricing.applyDiscount(manager.requireId(), "Bad", saleVehicle.requireId(), BigDecimal.TEN, DAY, DAY.minusDays(1)));
        assertEquals(new BigDecimal("27000.00"), pricing.quoteSale(saleVehicle.requireId(), DAY));
        var replaced = pricing.applyDiscount(manager.requireId(), "New", saleVehicle.requireId(), new BigDecimal("20"), DAY, DAY);
        assertEquals(discount.id(), replaced.id());
        pricing.removeDiscount(manager.requireId(), discount.requireId());
        assertEquals(new BigDecimal("30000.00"), pricing.quoteSale(saleVehicle.requireId(), DAY));
        assertThrows(DomainRuleViolationException.class, () -> pricing.updateDailyRentalRate(manager.requireId(), saleVehicle.requireId(), BigDecimal.TEN));
    }

    @ParameterizedTest @EnumSource(Role.class)
    @DisplayName("FT-AUTHZ-01: role enforcement on writes and private lists across all three roles")
    void ftAuthz01RoleMatrix(Role role) {
        long actor = switch (role) { case CUSTOMER -> customer.requireId(); case SALESMAN -> salesman.requireId(); case MANAGER -> manager.requireId(); };
        if (role != Role.CUSTOMER) {
            assertThrows(DomainRuleViolationException.class, () -> rentals.rentalsForCustomer(actor));
            assertThrows(DomainRuleViolationException.class, () -> rentals.requestRental(actor, rentalVehicle.requireId(), DAY, DAY.plusDays(1)));
            assertThrows(DomainRuleViolationException.class, () -> proposals.proposalsForCustomer(actor));
        }
        if (role != Role.SALESMAN) {
            assertThrows(DomainRuleViolationException.class, () -> rentals.unassignedRentals(actor));
            assertThrows(DomainRuleViolationException.class, () -> testDrives.unassignedBookings(actor));
            assertThrows(DomainRuleViolationException.class, () -> inventory.sendToMaintenance(actor, rentalVehicle.requireId()));
            assertThrows(DomainRuleViolationException.class, () -> proposals.proposalsForSalesman(actor));
        }
        if (role != Role.MANAGER) {
            assertThrows(DomainRuleViolationException.class, () -> inventory.stockOrders(actor));
            assertThrows(DomainRuleViolationException.class, () -> proposals.awaitingManagerDecision(actor));
            assertThrows(DomainRuleViolationException.class, () -> pricing.updateSalePrice(actor, saleVehicle.requireId(), BigDecimal.TEN));
            assertThrows(DomainRuleViolationException.class, () -> inventory.placeStockOrder(actor, model.requireId(), 1, BigDecimal.TEN));
        }
        if (role == Role.CUSTOMER) assertThrows(DomainRuleViolationException.class, () -> inventory.inventory(actor));
    }
}
