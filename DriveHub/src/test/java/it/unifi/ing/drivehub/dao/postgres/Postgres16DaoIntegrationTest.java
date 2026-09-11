package it.unifi.ing.drivehub.dao.postgres;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/** Repeats all five DAO contracts on PostgreSQL itself, independently of the H2 suite. */
@Tag("postgres")
class Postgres16DaoIntegrationTest extends PostgresDaoIntegrationTest {
    private PostgresTestDatabase database;

    @Override
    protected javax.sql.DataSource createDataSource() {
        try {
            database = new PostgresTestDatabase();
            return database.dataSource();
        } catch (java.sql.SQLException failure) {
            throw new AssertionError(failure);
        }
    }

    @AfterEach
    void removeTestSchema() throws java.sql.SQLException {
        if (database != null) database.close();
    }
}
