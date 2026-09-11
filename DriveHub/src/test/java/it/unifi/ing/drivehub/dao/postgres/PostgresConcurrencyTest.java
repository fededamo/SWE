package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.business.core.*;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.services.*;
import it.unifi.ing.drivehub.business.strategies.StandardPricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.*;
import it.unifi.ing.drivehub.domain.sales.*;
import it.unifi.ing.drivehub.domain.users.*;
import it.unifi.ing.drivehub.domain.vehicles.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Tag("postgres")
class PostgresConcurrencyTest {
    private PostgresTestDatabase database;
    private DaoFactory factory;
    private RentalService rentals;
    private SalesService sales;
    private TestDriveService drives;
    private PurchaseProposalService proposals;
    private PaymentService payments;
    private CheckoutService checkout;
    private User customer, other, salesman, secondSalesman, manager;
    private Vehicle sale, rent;
    private final AtomicInteger charges = new AtomicInteger();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate DAY = LocalDate.of(2027, 1, 10);

    @BeforeEach void initialize() throws SQLException {
        database = new PostgresTestDatabase(); new DatabaseBootstrap(database.dataSource(), true).initialize();
        factory = new PostgresDaoFactory(database.dataSource());
        var strategy = new StandardPricingStrategy(); rentals = new RentalService(factory, strategy); sales = new SalesService(factory, strategy);
        drives = new TestDriveService(factory, CLOCK); proposals = new PurchaseProposalService(factory, CLOCK);
        payments = new PaymentService(factory, (amount, method) -> PaymentGatewayResult.approved("pg-" + charges.incrementAndGet()), CLOCK);
        checkout = new CheckoutService(factory, rentals, sales, payments);
        try (var u = factory.begin()) {
            customer = u.users().save(user(1, Role.CUSTOMER)); other = u.users().save(user(2, Role.CUSTOMER));
            salesman = u.users().save(user(3, Role.SALESMAN)); secondSalesman = u.users().save(user(4, Role.SALESMAN)); manager = u.users().save(user(5, Role.MANAGER));
            sale = u.vehicles().findAvailableForSale().getFirst(); rent = u.vehicles().findAvailableForRental().getFirst(); u.commit();
        }
    }
    @AfterEach void cleanup() throws SQLException { if (database != null) database.close(); }

    @Test @DisplayName("IT-CON-01: concurrent sale checkouts charge once and persist one order")
    void doubleSale() throws Exception {
        BigDecimal price = sale.salePrice();
        oneWinner(() -> checkout.checkoutSale(customer.requireId(), sale.requireId(), true, PaymentMethod.CARD, price, DAY),
                () -> checkout.checkoutSale(other.requireId(), sale.requireId(), true, PaymentMethod.CARD, price, DAY));
        assertEquals(1, charges.get()); assertEquals(1, count("sale_orders")); assertEquals(1, count("payments"));
    }
    @Test @DisplayName("IT-CON-02: concurrent payment of the same rental charges once")
    void doubleRentalPayment() throws Exception {
        var rental = rentals.requestRental(customer.requireId(), rent.requireId(), DAY, DAY.plusDays(1));
        oneWinner(() -> payments.payRental(customer.requireId(), rental.requireId(), PaymentMethod.CARD),
                () -> payments.payRental(customer.requireId(), rental.requireId(), PaymentMethod.CARD));
        assertEquals(1, charges.get()); assertEquals(1, count("payments"));
    }
    @Test @DisplayName("IT-CON-03: concurrent balance payments preserve exact accumulated amount")
    void doubleBalance() throws Exception {
        BigDecimal deposit = new BigDecimal("1000.00");
        var order = sales.reserveVehicle(customer.requireId(), sale.requireId(), deposit, DAY);
        payments.paySaleOrder(customer.requireId(), order.requireId(), PaymentPurpose.SALE_DEPOSIT, PaymentMethod.CARD, deposit);
        var balance = order.totalPrice().subtract(deposit);
        oneWinner(() -> payments.paySaleOrder(customer.requireId(), order.requireId(), PaymentPurpose.SALE_BALANCE, PaymentMethod.CARD, balance),
                () -> payments.paySaleOrder(customer.requireId(), order.requireId(), PaymentPurpose.SALE_BALANCE, PaymentMethod.CARD, balance));
        assertEquals(2, charges.get());
        assertEquals(SaleOrderStatus.PAID, sales.ordersForCustomer(customer.requireId()).getFirst().status());
    }
    @Test @DisplayName("IT-CON-04: overlapping slots on different vehicles serialize by customer")
    void sameCustomerDifferentVehicles() throws Exception {
        Vehicle second;
        try (var u = factory.begin()) { second = u.vehicles().save(Vehicle.create("CO200AA", sale.model(), VehiclePurpose.FOR_SALE, sale.salePrice(), null, 0)); u.commit(); }
        oneWinner(() -> drives.book(customer.requireId(), sale.requireId(), DAY.atTime(10, 0), Duration.ofHours(1)),
                () -> drives.book(customer.requireId(), second.requireId(), DAY.atTime(10, 30), Duration.ofHours(1)));
        assertEquals(1, count("test_drives"));
    }
    @Test @DisplayName("IT-CON-05: overlapping rentals with different start dates have one winner")
    void overlappingRentalRequests() throws Exception {
        oneWinner(() -> rentals.requestRental(customer.requireId(), rent.requireId(), DAY, DAY.plusDays(3)),
                () -> rentals.requestRental(other.requireId(), rent.requireId(), DAY.plusDays(1), DAY.plusDays(4)));
        assertEquals(1, count("rentals"));
    }
    @Test @DisplayName("IT-CON-06: two staff claims and two proposal decisions each have one winner")
    void claimAndDecisionCas() throws Exception {
        var rental = rentals.requestRental(customer.requireId(), rent.requireId(), DAY, DAY.plusDays(1));
        oneWinner(() -> rentals.claimRental(rental.requireId(), salesman.requireId()),
                () -> rentals.claimRental(rental.requireId(), secondSalesman.requireId()));
        var proposal = proposals.request(customer.requireId(), sale.model().requireId(), "CO300AA", 0, BigDecimal.TEN, "Demo");
        proposals.submitOffer(proposal.requireId(), salesman.requireId(), BigDecimal.TEN, "Terms");
        oneWinner(() -> proposals.approve(proposal.requireId(), manager.requireId(), "Approved"),
                () -> proposals.reject(proposal.requireId(), manager.requireId(), "Refused"));
        assertEquals(CLOCK.instant(), proposals.proposalsForCustomer(customer.requireId()).getFirst().decidedAt());
    }
    @Test @DisplayName("IT-CON-07: SQL failure after gateway approval rolls back rental and payment")
    void persistenceFailureAfterApproval() throws SQLException {
        // Intercept only the payment update. Every preceding write uses the real PostgreSQL transaction.
        DaoFactory failing = () -> {
            UnitOfWork real = factory.begin();
            PaymentDao realPayments = real.payments();
            PaymentDao failure = (PaymentDao) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PaymentDao.class}, (p, method, args) -> {
                if (method.getName().equals("update")) throw new DatabaseException("injected persistence failure", new SQLException("injected", "23514"));
                return invoke(realPayments, method, args);
            });
            return (UnitOfWork) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UnitOfWork.class}, (p, method, args) ->
                    method.getName().equals("payments") ? failure : invoke(real, method, args));
        };
        CheckoutService failingCheckout = new CheckoutService(failing, rentals, sales, payments);
        assertThrows(DatabaseException.class, () -> failingCheckout.checkoutRental(customer.requireId(), rent.requireId(), DAY,
                DAY.plusDays(1), PaymentMethod.CARD, rent.dailyRentalRate()));
        assertEquals(1, charges.get()); assertEquals(0, count("rentals")); assertEquals(0, count("payments"));
    }
    @Test @DisplayName("IT-CON-08: PostgreSQL partial uniqueness permits rebooking a cancelled slot")
    void cancelledSlotCanBeRebooked() {
        var first = drives.book(customer.requireId(), sale.requireId(), DAY.atTime(10, 0), Duration.ofHours(1));
        drives.cancel(first.requireId(), customer.requireId());
        assertDoesNotThrow(() -> drives.book(customer.requireId(), sale.requireId(), DAY.atTime(10, 0), Duration.ofHours(1)));
    }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); } catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    private static void oneWinner(Callable<?> first, Callable<?> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> attempt(first, barrier)); var b = executor.submit(() -> attempt(second, barrier));
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }
    private static boolean attempt(Callable<?> action, CyclicBarrier barrier) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try { action.call(); return true; } catch (ConflictException expected) { return false; }
    }
    private long count(String table) throws SQLException {
        try (var c = database.dataSource().getConnection(); var s = c.createStatement(); var r = s.executeQuery("SELECT count(*) FROM " + table)) { r.next(); return r.getLong(1); }
    }
    private static User user(int n, Role role) {
        return User.register(String.format("PGUSER%010d", n), "Fixture", "User", "pg" + n + "@example.invalid", "+39000000000", "fixture-hash", role, null);
    }
}
