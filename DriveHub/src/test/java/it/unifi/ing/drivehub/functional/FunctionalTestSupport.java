package it.unifi.ing.drivehub.functional;

import it.unifi.ing.drivehub.business.core.PaymentGateway;
import it.unifi.ing.drivehub.business.core.PaymentGatewayResult;
import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.security.Pbkdf2PasswordHasher;
import it.unifi.ing.drivehub.business.services.*;
import it.unifi.ing.drivehub.business.strategies.StandardPricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.postgres.DatabaseBootstrap;
import it.unifi.ing.drivehub.dao.postgres.PostgresDaoFactory;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** UC-driven black-box service scenarios; real JDBC/H2, not evidence of PostgreSQL equivalence. */
abstract class FunctionalTestSupport {
    protected static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T10:00:00Z"), ZoneOffset.UTC);
    protected static final LocalDate DAY = LocalDate.of(2027, 1, 10);
    protected static final BigDecimal RENT_TOTAL = new BigDecimal("300.00");
    protected DaoFactory factory;
    protected TransactionRunner transactions;
    protected AuthService auth;
    protected RentalService rentals;
    protected SalesService sales;
    protected TestDriveService testDrives;
    protected PurchaseProposalService proposals;
    protected InventoryService inventory;
    protected PricingService pricing;
    protected DashboardService dashboard;
    protected PaymentService payments;
    protected CheckoutService checkout;
    protected User customer, otherCustomer, salesman, otherSalesman, manager;
    protected Vehicle rentalVehicle, saleVehicle;
    protected VehicleModel model;
    protected final AtomicInteger charges = new AtomicInteger();

    @BeforeEach
    void createIsolatedApplication() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:ft_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        source.setUser("sa");
        source.setPassword("");
        new DatabaseBootstrap(source, false).initialize();
        factory = new PostgresDaoFactory(source);
        transactions = new TransactionRunner(factory);
        StandardPricingStrategy strategy = new StandardPricingStrategy();
        auth = new AuthService(factory, new Pbkdf2PasswordHasher(10_000, 16, 128, new SecureRandom()));
        rentals = new RentalService(factory, strategy);
        sales = new SalesService(factory, strategy);
        testDrives = new TestDriveService(factory, CLOCK);
        proposals = new PurchaseProposalService(factory, CLOCK);
        inventory = new InventoryService(factory, CLOCK);
        pricing = new PricingService(factory, strategy);
        dashboard = new DashboardService(factory);
        withGateway((amount, method) -> PaymentGatewayResult.approved("test-" + charges.incrementAndGet()));
        customer = register(1, Role.CUSTOMER);
        otherCustomer = register(2, Role.CUSTOMER);
        salesman = register(3, Role.SALESMAN);
        otherSalesman = register(4, Role.SALESMAN);
        manager = register(5, Role.MANAGER);
        Brand brand = inventory.addBrand(salesman.requireId(), "Example Motors");
        model = inventory.addModel(salesman.requireId(), brand.requireId(), "EXAMPLE-2026", "Example", 2026);
        rentalVehicle = inventory.addVehicle(salesman.requireId(), model.requireId(), "RE100AA",
                VehiclePurpose.RENTAL, null, new BigDecimal("100.00"), 1000);
        saleVehicle = inventory.addVehicle(salesman.requireId(), model.requireId(), "SA100AA",
                VehiclePurpose.FOR_SALE, new BigDecimal("40000.00"), null, 1000);
    }

    protected User register(int number, Role role) {
        return auth.register(new RegistrationRequest(String.format("TESTUSER%08d", number), "Demo", "User",
                "demo" + number + "@example.test", "+390550000000", role, null), "test-pass-123".toCharArray());
    }

    protected void withGateway(PaymentGateway gateway) {
        payments = new PaymentService(factory, gateway, CLOCK);
        checkout = new CheckoutService(factory, rentals, sales, payments);
    }

    protected Rental rental(long id) {
        return transactions.execute(unit -> unit.rentals().findById(id).orElseThrow());
    }

    protected SaleOrder order(long id) {
        return transactions.execute(unit -> unit.saleOrders().findById(id).orElseThrow());
    }

    protected Vehicle vehicle(long id) {
        return transactions.execute(unit -> unit.vehicles().findById(id).orElseThrow());
    }
}
