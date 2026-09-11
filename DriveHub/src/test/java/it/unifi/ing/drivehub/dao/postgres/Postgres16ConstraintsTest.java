package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.rentals.*;
import it.unifi.ing.drivehub.domain.sales.*;
import it.unifi.ing.drivehub.domain.users.*;
import it.unifi.ing.drivehub.domain.vehicles.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Tag("postgres")
class Postgres16ConstraintsTest {
    private PostgresTestDatabase database;
    private DaoFactory factory;
    private User customer;
    private User salesman;
    private User manager;
    private Vehicle saleVehicle;
    private Vehicle rentalVehicle;

    @BeforeEach
    void initialize() throws SQLException {
        database = new PostgresTestDatabase();
        new DatabaseBootstrap(database.dataSource(), true).initialize();
        factory = new PostgresDaoFactory(database.dataSource());
        try (var unit = factory.begin()) {
            customer = unit.users().save(user("TSTCST80A01H501A", "customer", Role.CUSTOMER));
            salesman = unit.users().save(user("TSTSLM80A01H501A", "salesman", Role.SALESMAN));
            manager = unit.users().save(user("TSTMGR80A01H501A", "manager", Role.MANAGER));
            saleVehicle = unit.vehicles().findAvailableForSale().getFirst();
            rentalVehicle = unit.vehicles().findAvailableForRental().getFirst();
            unit.commit();
        }
    }

    @AfterEach
    void cleanup() throws SQLException { if (database != null) database.close(); }

    @Test
    @DisplayName("IT-PG-01: migration da schema vuoto, seed idempotente, indici e FK reali")
    void migrationSeedAndCatalogAreRealAndIdempotent() throws SQLException {
        new DatabaseBootstrap(database.dataSource(), true).initialize();
        assertEquals(5, scalar("SELECT count(*) FROM drivehub_schema_migrations"));
        assertEquals(2, scalar("SELECT count(*) FROM vehicles"));
        assertEquals(3, scalar("SELECT count(*) FROM users"));
        assertEquals(22, scalar("SELECT count(*) FROM pg_indexes WHERE schemaname=current_schema() AND indexname LIKE 'idx_%'"));
        assertEquals(22, scalar("SELECT count(*) FROM pg_constraint WHERE connamespace=current_schema()::regnamespace AND contype='f'"));
    }

    @Test
    @DisplayName("IT-PG-02: tutti i literal Java corrispondono ai CHECK PostgreSQL")
    void allEnumLiteralsMatchActualCheckConstraints() throws SQLException {
        Map<String, Class<? extends Enum<?>>> constraints = Map.ofEntries(
                Map.entry("ck_users_role", Role.class),
                Map.entry("ck_vehicles_purpose", VehiclePurpose.class),
                Map.entry("ck_vehicles_status", VehicleStatus.class),
                Map.entry("ck_rentals_status", RentalStatus.class),
                Map.entry("ck_test_drives_status", TestDriveStatus.class),
                Map.entry("ck_sale_orders_status", SaleOrderStatus.class),
                Map.entry("ck_payments_purpose", PaymentPurpose.class),
                Map.entry("ck_payments_method", PaymentMethod.class),
                Map.entry("ck_payments_status", PaymentStatus.class),
                Map.entry("ck_purchase_proposals_status", PurchaseProposalStatus.class),
                Map.entry("ck_stock_orders_status", StockOrderStatus.class));
        try (var connection = database.dataSource().getConnection();
             var query = connection.prepareStatement("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE connamespace=current_schema()::regnamespace AND conname=?")) {
            for (var entry : constraints.entrySet()) {
                query.setString(1, entry.getKey());
                try (var result = query.executeQuery()) {
                    assertTrue(result.next(), entry.getKey());
                    var matcher = Pattern.compile("'([A-Z_]+)'").matcher(result.getString(1));
                    Set<String> stored = new TreeSet<>();
                    while (matcher.find()) stored.add(matcher.group(1));
                    Set<String> java = new TreeSet<>();
                    for (Enum<?> literal : entry.getValue().getEnumConstants()) java.add(literal.name());
                    assertEquals(java, stored, entry.getKey());
                }
            }
        }
    }

    @Test
    @DisplayName("IT-PG-03: FK, unique, NULL nei CHECK e importi non validi")
    void foreignKeysUniqueAndNullSensitiveChecksRejectInvalidRows() throws SQLException {
        assertSqlState("23503", "INSERT INTO vehicle_models(brand_id,code,name,model_year) VALUES(999999,'INVALID','Invalid',2026)");
        assertSqlState("23505", "INSERT INTO brands(name) VALUES('Demo Motors')");
        assertSqlState("23514", "UPDATE vehicles SET sale_price=NULL WHERE purpose='FOR_SALE'");
        assertSqlState("23514", "UPDATE vehicles SET daily_rental_rate=NULL WHERE purpose='RENTAL'");
        assertSqlState("23514", "UPDATE users SET role='ADMIN' WHERE id=" + customer.requireId());
        assertSqlState("23514", "INSERT INTO discounts(name,vehicle_id,percentage,starts_on,ends_on) VALUES('Invalid'," + saleVehicle.requireId() + ",101,CURRENT_DATE,CURRENT_DATE)");
        assertSqlState("23514", "INSERT INTO payments(payer_id,purpose,method,amount,created_at) VALUES(" + customer.requireId() + ",'RENTAL','CARD',10,CURRENT_TIMESTAMP)");
    }

    @Test
    @DisplayName("IT-PG-04: intervallo PostgreSQL, adiacenza e overlap di test drive")
    void intervalCheckAndHalfOpenOverlapQuery() throws SQLException {
        var start = LocalDateTime.of(2027, 1, 10, 10, 0);
        try (var unit = factory.begin()) {
            unit.testDrives().save(TestDrive.request(customer, saleVehicle, start, Duration.ofHours(1)));
            assertTrue(unit.testDrives().existsOverlapping(saleVehicle.requireId(), start.plusMinutes(59), start.plusHours(2)));
            assertFalse(unit.testDrives().existsOverlapping(saleVehicle.requireId(), start.plusHours(1), start.plusHours(2)));
            unit.commit();
        }
        assertSqlState("23514", "UPDATE test_drives SET ends_at=scheduled_at+INTERVAL '4 hours 1 minute'");
    }

    @Test
    @DisplayName("IT-PG-05: due connessioni concorrenti, un solo vincitore CAS")
    void concurrentVehicleCasHasExactlyOneWinner() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> reserve = () -> {
                try (var unit = factory.begin()) {
                    barrier.await(5, TimeUnit.SECONDS);
                    boolean result = unit.vehicles().updateStatusIfCurrent(saleVehicle.requireId(), VehicleStatus.AVAILABLE, VehicleStatus.RESERVED);
                    unit.commit();
                    return result;
                }
            };
            var first = executor.submit(reserve);
            var second = executor.submit(reserve);
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("IT-PG-06: rollback di rental e pagamento dopo errore SQL")
    void constraintFailureRollsBackAllWrites() throws SQLException {
        assertThrows(DatabaseException.class, () -> {
            try (var unit = factory.begin()) {
                var rental = unit.rentals().save(Rental.request(customer, rentalVehicle, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3), new BigDecimal("118.00")));
                unit.payments().save(Payment.start(customer, PaymentReferenceType.RENTAL, rental.requireId(), PaymentPurpose.RENTAL, PaymentMethod.CARD, rental.totalPrice(), Instant.parse("2026-09-07T12:00:00Z")));
                unit.brands().save(Brand.create("Demo Motors"));
                unit.commit();
            }
        });
        assertEquals(0, scalar("SELECT count(*) FROM rentals"));
        assertEquals(0, scalar("SELECT count(*) FROM payments"));
    }

    @Test
    @DisplayName("IT-PG-07: timestamp di decisione e CAS persistiti insieme")
    void proposalDecisionTimestampSurvivesReloadAndSecondDecisionLoses() {
        long proposalId;
        var decisionTime = Instant.parse("2026-09-07T12:00:00Z");
        try (var unit = factory.begin()) {
            var vehicle = unit.vehicles().save(Vehicle.create("PG0003", saleVehicle.model(), VehiclePurpose.ACQUISITION_REQUEST, null, null, 10));
            proposalId = unit.purchaseProposals().save(PurchaseProposal.request(customer, vehicle, BigDecimal.TEN, "Fictitious request", decisionTime.minusSeconds(60))).requireId();
            assertTrue(unit.purchaseProposals().submitOfferIfRequested(proposalId, salesman.requireId(), BigDecimal.TEN, "Terms"));
            assertTrue(unit.purchaseProposals().decideIfOffered(proposalId, manager.requireId(), true, "Approved", decisionTime));
            assertFalse(unit.purchaseProposals().decideIfOffered(proposalId, manager.requireId(), false, "Repeated", decisionTime.plusSeconds(1)));
            unit.commit();
        }
        try (var unit = factory.begin()) {
            assertEquals(decisionTime, unit.purchaseProposals().findById(proposalId).orElseThrow().decidedAt());
        }
    }

    private long scalar(String sql) throws SQLException {
        try (var connection = database.dataSource().getConnection(); var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private void assertSqlState(String state, String sql) throws SQLException {
        try (var connection = database.dataSource().getConnection(); var statement = connection.createStatement()) {
            assertEquals(state, assertThrows(SQLException.class, () -> statement.executeUpdate(sql)).getSQLState());
        }
    }

    private static User user(String fiscalCode, String label, Role role) {
        return User.register(fiscalCode, "Fixture", label, label + "@example.invalid", "+39000000000", "test-only-hash", role, null);
    }
}
