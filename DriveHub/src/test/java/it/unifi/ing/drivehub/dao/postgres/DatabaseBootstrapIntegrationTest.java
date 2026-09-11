package it.unifi.ing.drivehub.dao.postgres;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBootstrapIntegrationTest extends PostgresDaoIntegrationSupport {

    @Test
    void bootstrapIsIdempotentAndTracksEachMigrationOnce() throws SQLException {
        DatabaseBootstrap bootstrap = new DatabaseBootstrap(dataSource, false);

        bootstrap.initialize();

        assertEquals(4, scalarLong("SELECT COUNT(*) FROM drivehub_schema_migrations"));
        assertEquals(12, scalarLong("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'users', 'brands', 'vehicle_models', 'vehicles', 'discounts', 'rentals',
                    'test_drives', 'sale_orders', 'payments', 'purchase_proposals', 'stock_orders',
                    'drivehub_schema_migrations'
                  )
                """));
    }

    @Test
    void optionalDemoSeedContainsOnlyFictitiousCatalogDataAndIsIdempotent() throws SQLException {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:seed_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        h2.setPassword("");
        DatabaseBootstrap bootstrap = new DatabaseBootstrap(h2, true);

        bootstrap.initialize();
        bootstrap.initialize();

        try (Connection connection = h2.getConnection()) {
            assertEquals(2, scalarLong(connection, "SELECT COUNT(*) FROM brands"));
            assertEquals(2, scalarLong(connection, "SELECT COUNT(*) FROM vehicle_models"));
            assertEquals(2, scalarLong(connection, "SELECT COUNT(*) FROM vehicles"));
            assertEquals(0, scalarLong(connection, "SELECT COUNT(*) FROM users"));
            assertTrue(scalarLong(connection,
                    "SELECT COUNT(*) FROM vehicles WHERE plate LIKE 'DH%'") > 0);
        }
    }

    @Test
    void databaseChecksRejectInvalidDiscountAndAmbiguousPaymentReference() throws SQLException {
        Fixture fixture = insertMinimalFixture();

        try (Connection connection = dataSource.getConnection()) {
            assertThrows(SQLException.class, () -> {
                try (var statement = connection.prepareStatement(
                        """
                        INSERT INTO discounts(name, vehicle_id, percentage, starts_on, ends_on)
                        VALUES ('Invalid Demo', ?, ?, CURRENT_DATE, CURRENT_DATE)
                        """)) {
                    statement.setLong(1, fixture.vehicleId());
                    statement.setBigDecimal(2, new BigDecimal("101.00"));
                    statement.executeUpdate();
                }
            });

            assertThrows(SQLException.class, () -> {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO payments(
                            payer_id, rental_id, sale_order_id, purpose, method, amount, created_at, status
                        ) VALUES (?, NULL, NULL, 'RENTAL', 'CARD', ?, CURRENT_TIMESTAMP, 'PENDING')
                        """)) {
                    statement.setLong(1, fixture.customerId());
                    statement.setBigDecimal(2, BigDecimal.TEN);
                    statement.executeUpdate();
                }
            });
        }
    }

    @Test
    void transactionManagerRollsBackTheWholeUnitOfWork() {
        JdbcTransactionManager transactions = new JdbcTransactionManager(dataSource);

        assertThrows(DatabaseException.class, () -> transactions.inTransaction(
                (JdbcTransactionManager.SqlAction) connection -> {
            try (var statement = connection.prepareStatement("INSERT INTO brands(name) VALUES (?)")) {
                statement.setString(1, "Rollback Demo");
                statement.executeUpdate();
            }
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(0, uncheckedScalarLong(
                "SELECT COUNT(*) FROM brands WHERE name = 'Rollback Demo'"));
    }

    private Fixture insertMinimalFixture() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            long customerId;
            try (var statement = connection.prepareStatement("""
                    INSERT INTO users(
                        fiscal_code, first_name, last_name, email, phone, password_hash, role
                    ) VALUES (?, ?, ?, ?, ?, ?, 'CUSTOMER')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "TSTCST80A01H501A");
                statement.setString(2, "Test");
                statement.setString(3, "Customer");
                statement.setString(4, "constraint@example.invalid");
                statement.setString(5, "+39000000999");
                statement.setString(6, "test-hash");
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    customerId = keys.getLong(1);
                }
            }
            long brandId;
            try (var statement = connection.prepareStatement(
                    "INSERT INTO brands(name) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "Constraint Demo");
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    brandId = keys.getLong(1);
                }
            }
            long modelId;
            try (var statement = connection.prepareStatement("""
                    INSERT INTO vehicle_models(brand_id, code, name, model_year)
                    VALUES (?, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, brandId);
                statement.setString(2, "CONSTRAINT-MODEL");
                statement.setString(3, "Constraint Model");
                statement.setInt(4, 2026);
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    modelId = keys.getLong(1);
                }
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO vehicles(plate, model_id, purpose, sale_price, mileage, status)
                    VALUES (?, ?, 'FOR_SALE', 10000.00, 0, 'AVAILABLE')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "DHC001");
                statement.setLong(2, modelId);
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new Fixture(customerId, modelId, keys.getLong(1));
                }
            }
        }
    }

    private long scalarLong(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return scalarLong(connection, sql);
        }
    }

    private long uncheckedScalarLong(String sql) {
        try {
            return scalarLong(sql);
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private static long scalarLong(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private record Fixture(long customerId, long modelId, long vehicleId) {
    }
}
