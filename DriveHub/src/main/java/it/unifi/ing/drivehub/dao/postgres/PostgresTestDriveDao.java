package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.TestDriveDao;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

final class PostgresTestDriveDao extends AbstractPostgresDao implements TestDriveDao {

    PostgresTestDriveDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public TestDrive save(TestDrive testDrive) {
        if (testDrive.id() != null) {
            throw new IllegalArgumentException("test drive is already persistent");
        }
        long id = execute("Could not save test drive", () -> JdbcSupport.insert(connection, """
                INSERT INTO test_drives(
                    customer_id, salesman_id, vehicle_id, scheduled_at, ends_at, status
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> bindTestDrive(statement, testDrive, false)));
        testDrive.assignId(id);
        return testDrive;
    }

    @Override
    public void update(TestDrive testDrive) {
        execute("Could not update test drive", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE test_drives
                SET customer_id = ?, salesman_id = ?, vehicle_id = ?, scheduled_at = ?, ends_at = ?, status = ?
                WHERE id = ?
                """, statement -> bindTestDrive(statement, testDrive, true)));
    }

    @Override
    public Optional<TestDrive> findById(long id) {
        return loader.testDrive(id);
    }

    @Override
    public Optional<TestDrive> findByIdForUpdate(long id) {
        return findOne("SELECT id FROM test_drives WHERE id = ? FOR UPDATE",
                statement -> statement.setLong(1, id), loader::testDrive,
                "Could not lock test_drives row");
    }

    @Override
    public List<TestDrive> findByCustomer(long customerId) {
        return findMany("SELECT id FROM test_drives WHERE customer_id = ? ORDER BY scheduled_at DESC, id",
                statement -> statement.setLong(1, customerId), loader::testDrive,
                "Could not list test drives by customer");
    }

    @Override
    public List<TestDrive> findBySalesman(long salesmanId) {
        return findMany("SELECT id FROM test_drives WHERE salesman_id = ? ORDER BY scheduled_at DESC, id",
                statement -> statement.setLong(1, salesmanId), loader::testDrive,
                "Could not list test drives by salesman");
    }

    @Override
    public List<TestDrive> findUnassigned() {
        return findMany("""
                SELECT id FROM test_drives
                WHERE salesman_id IS NULL AND status = 'REQUESTED'
                ORDER BY scheduled_at, id
                """, statement -> { }, loader::testDrive, "Could not list unassigned test drives");
    }

    @Override
    public boolean existsOverlapping(long vehicleId, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("overlap interval must have positive duration");
        }
        return execute("Could not check overlapping test drives", () -> JdbcSupport.queryId(connection, """
                SELECT id FROM test_drives
                WHERE vehicle_id = ? AND status NOT IN ('CANCELLED', 'COMPLETED')
                  AND scheduled_at < ? AND ends_at > ?
                FETCH FIRST 1 ROW ONLY
                """, statement -> {
            statement.setObject(1, vehicleId);
            statement.setObject(2, endsAt);
            statement.setObject(3, startsAt);
        }).isPresent());
    }

    @Override
    public boolean confirmIfUnassigned(long testDriveId, long salesmanId) {
        return execute("Could not atomically confirm test drive", () -> JdbcSupport.update(connection, """
                UPDATE test_drives
                SET salesman_id = ?, status = 'CONFIRMED'
                WHERE id = ? AND salesman_id IS NULL AND status = 'REQUESTED'
                  AND EXISTS (
                    SELECT 1 FROM users WHERE id = ? AND role = 'SALESMAN' AND active = TRUE
                  )
                """, statement -> {
            statement.setLong(1, salesmanId);
            statement.setLong(2, testDriveId);
            statement.setLong(3, salesmanId);
        }) == 1);
    }

    private static void bindTestDrive(
            java.sql.PreparedStatement statement, TestDrive testDrive, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, testDrive.customer().requireId());
        JdbcSupport.setNullableLong(statement, 2,
                testDrive.salesman() == null ? null : testDrive.salesman().requireId());
        statement.setLong(3, testDrive.vehicle().requireId());
        statement.setObject(4, testDrive.scheduledAt());
        statement.setObject(5, testDrive.endsAt());
        statement.setString(6, testDrive.status().name());
        if (includeId) {
            statement.setLong(7, testDrive.requireId());
        }
    }
}
