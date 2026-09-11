package it.unifi.ing.drivehub.dao.postgres;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

/** Every real-engine test owns a new schema; existing application schemas are never touched. */
public final class PostgresTestDatabase implements AutoCloseable {
    private final PGSimpleDataSource admin = new PGSimpleDataSource();
    private final PGSimpleDataSource scoped = new PGSimpleDataSource();
    private final String schema = "verification_" + UUID.randomUUID().toString().replace("-", "");

    public PostgresTestDatabase() throws SQLException {
        String url = required("DRIVEHUB_TEST_DB_URL");
        String user = required("DRIVEHUB_TEST_DB_USER");
        String password = required("DRIVEHUB_TEST_DB_PASSWORD");
        admin.setURL(url);
        admin.setUser(user);
        admin.setPassword(password);
        try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
            if (connection.getMetaData().getDatabaseMajorVersion() != 16) {
                throw new IllegalStateException("The postgres profile requires PostgreSQL 16");
            }
            statement.execute("CREATE SCHEMA " + schema);
        }
        scoped.setURL(url);
        scoped.setUser(user);
        scoped.setPassword(password);
        scoped.setCurrentSchema(schema);
    }

    public DataSource dataSource() { return scoped; }

    public String jdbcUrl() {
        String base = required("DRIVEHUB_TEST_DB_URL");
        return base + (base.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    }

    @Override
    public void close() throws SQLException {
        try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for the postgres profile; use scripts/test_postgres.sh");
        }
        return value;
    }
}
