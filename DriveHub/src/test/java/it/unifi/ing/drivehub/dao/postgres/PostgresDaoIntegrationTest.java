package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.rentals.TestDriveStatus;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposalStatus;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.StockOrderStatus;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresDaoIntegrationTest extends PostgresDaoIntegrationSupport {

    private DaoFactory daoFactory;

    @BeforeEach
    void createFactory() {
        daoFactory = new PostgresDaoFactory(dataSource);
    }

    @Test
    void persistsAndRehydratesUsersCatalogAndVehicleDiscount() {
        Fixture fixture = createFixture();
        LocalDate today = LocalDate.of(2026, 8, 23);

        try (var work = daoFactory.begin()) {
            Discount discount = Discount.create("Demo sale", fixture.saleVehicle(),
                    new BigDecimal("12.50"), today.minusDays(1), today.plusDays(1));
            assertSame(discount, work.discounts().save(discount));
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            User salesman = work.users().findByEmail(fixture.salesman().email()).orElseThrow();
            assertEquals(fixture.manager().id(), salesman.managerId());
            assertEquals(fixture.customer().id(),
                    work.users().findByFiscalCode(fixture.customer().fiscalCode()).orElseThrow().id());
            assertEquals(1, work.users().findByRole(Role.MANAGER).size());

            VehicleModel model = work.vehicleModels().findByCode(fixture.model().code()).orElseThrow();
            assertEquals(fixture.brand().id(), model.brand().id());
            assertEquals(3, work.vehicles().findByModel(model.requireId()).size());
            assertEquals(fixture.saleVehicle().id(), work.vehicles().findByPlate("aa-000-aa").orElseThrow().id());
            assertEquals(1, work.vehicles().findAvailableForSale().size());
            assertEquals(1, work.vehicles().findAvailableForRental().size());

            Discount loaded = work.discounts().findActiveOn(today).getFirst();
            assertEquals(fixture.saleVehicle().id(), loaded.vehicle().id());
            assertEquals(new BigDecimal("12.50"), loaded.percentage());
            work.commit();
        }
    }

    @Test
    void rentalAndPaymentCanShareOneTransactionAndClaimIsCompareAndSet() {
        Fixture fixture = createFixture();
        Rental rental;
        Payment payment;

        try (var work = daoFactory.begin()) {
            rental = Rental.request(fixture.customer(), fixture.rentalVehicle(),
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), new BigDecimal("236.00"));
            work.rentals().save(rental);
            payment = Payment.start(fixture.customer(), PaymentReferenceType.RENTAL, rental.requireId(),
                    PaymentPurpose.RENTAL, PaymentMethod.CARD, rental.totalPrice(),
                    Instant.parse("2026-08-23T12:00:00Z"));
            work.payments().save(payment);
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            assertTrue(work.rentals().claimIfUnassigned(rental.requireId(), fixture.salesman().requireId()));
            assertFalse(work.rentals().claimIfUnassigned(rental.requireId(), fixture.salesman().requireId()));
            Payment loadedPayment = work.payments().findByReference(
                    PaymentReferenceType.RENTAL, rental.requireId()).getFirst();
            loadedPayment.complete("DEMO-PROCESSOR-001");
            work.payments().update(loadedPayment);
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            Rental claimed = work.rentals().findById(rental.requireId()).orElseThrow();
            assertEquals(RentalStatus.ASSIGNED, claimed.status());
            assertEquals(fixture.salesman().id(), claimed.salesman().id());
            assertEquals(PaymentStatus.COMPLETED,
                    work.payments().findById(payment.requireId()).orElseThrow().status());
            work.commit();
        }
    }

    @Test
    void closingWithoutCommitRollsBackEveryWriteInTheUnitOfWork() {
        Fixture fixture = createFixture();
        Rental rental = Rental.request(fixture.customer(), fixture.rentalVehicle(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), new BigDecimal("118.00"));

        try (var work = daoFactory.begin()) {
            work.rentals().save(rental);
            work.payments().save(Payment.start(fixture.customer(), PaymentReferenceType.RENTAL,
                    rental.requireId(), PaymentPurpose.RENTAL, PaymentMethod.CARD,
                    rental.totalPrice(), Instant.parse("2026-08-23T12:30:00Z")));
        }

        try (var work = daoFactory.begin()) {
            assertTrue(work.rentals().findAll().isEmpty());
            assertTrue(work.payments().findAll().isEmpty());
            work.commit();
        }
    }

    @Test
    void testDriveOverlapAndConfirmationAreAtomic() {
        Fixture fixture = createFixture();
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 10, 10, 0);
        TestDrive testDrive;

        try (var work = daoFactory.begin()) {
            testDrive = TestDrive.request(fixture.customer(), fixture.saleVehicle(), startsAt,
                    Duration.ofMinutes(45));
            work.testDrives().save(testDrive);
            assertTrue(work.testDrives().existsOverlapping(fixture.saleVehicle().requireId(),
                    startsAt.plusMinutes(30), startsAt.plusHours(1)));
            assertFalse(work.testDrives().existsOverlapping(fixture.saleVehicle().requireId(),
                    startsAt.plusHours(1), startsAt.plusHours(2)));
            assertTrue(work.testDrives().confirmIfUnassigned(
                    testDrive.requireId(), fixture.salesman().requireId()));
            assertFalse(work.testDrives().confirmIfUnassigned(
                    testDrive.requireId(), fixture.salesman().requireId()));
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            TestDrive loaded = work.testDrives().findById(testDrive.requireId()).orElseThrow();
            assertEquals(TestDriveStatus.CONFIRMED, loaded.status());
            assertEquals(fixture.salesman().id(), loaded.salesman().id());
            work.commit();
        }
    }

    @Test
    void saleProposalAndStockOrderRoundTripThroughTheirCasWorkflows() {
        Fixture fixture = createFixture();
        PurchaseProposal proposal;
        SaleOrder saleOrder;
        StockOrder stockOrder;

        try (var work = daoFactory.begin()) {
            fixture.saleVehicle().reserveForSale();
            work.vehicles().update(fixture.saleVehicle());
            saleOrder = new SaleOrder(null, fixture.customer(), null, fixture.saleVehicle(),
                    new BigDecimal("20000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                    it.unifi.ing.drivehub.domain.sales.SaleOrderStatus.RESERVED);
            work.saleOrders().save(saleOrder);

            proposal = PurchaseProposal.request(fixture.customer(), fixture.requestVehicle(),
                    new BigDecimal("10000.00"), "Fictitious acquisition request",
                    Instant.parse("2026-08-23T13:00:00Z"));
            work.purchaseProposals().save(proposal);

            stockOrder = StockOrder.place(fixture.manager(), fixture.model(), 2,
                    new BigDecimal("15000.00"), LocalDate.of(2026, 8, 23));
            work.stockOrders().save(stockOrder);
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            assertTrue(work.saleOrders().claimIfUnassigned(
                    saleOrder.requireId(), fixture.salesman().requireId()));
            assertTrue(work.purchaseProposals().submitOfferIfRequested(
                    proposal.requireId(), fixture.salesman().requireId(),
                    new BigDecimal("9000.00"), "Demo offer terms"));
            assertFalse(work.purchaseProposals().submitOfferIfRequested(
                    proposal.requireId(), fixture.salesman().requireId(),
                    new BigDecimal("9000.00"), "Repeated offer"));
            assertTrue(work.purchaseProposals().decideIfOffered(
                    proposal.requireId(), fixture.manager().requireId(), true, "Approved for demo"));
            assertFalse(work.purchaseProposals().decideIfOffered(
                    proposal.requireId(), fixture.manager().requireId(), false, "Repeated decision"));
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            assertEquals(fixture.salesman().id(),
                    work.saleOrders().findById(saleOrder.requireId()).orElseThrow().salesman().id());
            assertEquals(PurchaseProposalStatus.APPROVED,
                    work.purchaseProposals().findById(proposal.requireId()).orElseThrow().status());
            assertEquals(StockOrderStatus.PLACED,
                    work.stockOrders().findById(stockOrder.requireId()).orElseThrow().status());
            assertEquals(1, work.stockOrders().findAll().size());
            work.commit();
        }
    }

    private Fixture createFixture() {
        try (var work = daoFactory.begin()) {
            User manager = User.register("RSSMRA80A01H501U", "Mario", "Rossi",
                    "manager@example.invalid", "+39000000001", "demo-hash", Role.MANAGER, null);
            work.users().save(manager);
            User salesman = User.register("BNCLGU80A01H501V", "Luigi", "Bianchi",
                    "salesman@example.invalid", "+39000000002", "demo-hash", Role.SALESMAN,
                    manager.requireId());
            work.users().save(salesman);
            User customer = User.register("VRDLGI80A01H501W", "Giulia", "Verdi",
                    "customer@example.invalid", "+39000000003", "demo-hash", Role.CUSTOMER, null);
            work.users().save(customer);

            Brand brand = Brand.create("Fixture Brand");
            work.brands().save(brand);
            VehicleModel model = VehicleModel.create(brand, "FIXTURE-2026", "Fixture Model", 2026);
            work.vehicleModels().save(model);
            Vehicle saleVehicle = Vehicle.create("AA000AA", model, VehiclePurpose.FOR_SALE,
                    new BigDecimal("20000.00"), null, 1000);
            Vehicle rentalVehicle = Vehicle.create("BB000BB", model, VehiclePurpose.RENTAL,
                    null, new BigDecimal("59.00"), 2000);
            Vehicle requestVehicle = Vehicle.create("CC000CC", model,
                    VehiclePurpose.ACQUISITION_REQUEST, null, null, 50000);
            work.vehicles().save(saleVehicle);
            work.vehicles().save(rentalVehicle);
            work.vehicles().save(requestVehicle);
            work.commit();
            return new Fixture(manager, salesman, customer, brand, model,
                    saleVehicle, rentalVehicle, requestVehicle);
        }
    }

    private record Fixture(
            User manager,
            User salesman,
            User customer,
            Brand brand,
            VehicleModel model,
            Vehicle saleVehicle,
            Vehicle rentalVehicle,
            Vehicle requestVehicle) {
    }
}
