package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.RentalDao;
import it.unifi.ing.drivehub.domain.rentals.Rental;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class PostgresRentalDao extends AbstractPostgresDao implements RentalDao {

    PostgresRentalDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public Rental save(Rental rental) {
        if (rental.id() != null) {
            throw new IllegalArgumentException("rental is already persistent");
        }
        long id = execute("Could not save rental", () -> JdbcSupport.insert(connection, """
                INSERT INTO rentals(
                    customer_id, salesman_id, vehicle_id, starts_on, ends_on, total_price, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindRental(statement, rental, false)));
        rental.assignId(id);
        return rental;
    }

    @Override
    public void update(Rental rental) {
        execute("Could not update rental", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE rentals
                SET customer_id = ?, salesman_id = ?, vehicle_id = ?, starts_on = ?, ends_on = ?,
                    total_price = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, statement -> bindRental(statement, rental, true)));
    }

    @Override
    public Optional<Rental> findById(long id) {
        return loader.rental(id);
    }

    @Override
    public List<Rental> findAll() {
        return findMany("SELECT id FROM rentals ORDER BY starts_on DESC, id", statement -> { },
                loader::rental, "Could not list rentals");
    }

    @Override
    public List<Rental> findByCustomer(long customerId) {
        return findMany("SELECT id FROM rentals WHERE customer_id = ? ORDER BY starts_on DESC, id",
                statement -> statement.setLong(1, customerId), loader::rental,
                "Could not list rentals by customer");
    }

    @Override
    public List<Rental> findBySalesman(long salesmanId) {
        return findMany("SELECT id FROM rentals WHERE salesman_id = ? ORDER BY starts_on DESC, id",
                statement -> statement.setLong(1, salesmanId), loader::rental,
                "Could not list rentals by salesman");
    }

    @Override
    public List<Rental> findUnassigned() {
        return findMany("""
                SELECT id FROM rentals
                WHERE salesman_id IS NULL AND status = 'REQUESTED'
                ORDER BY created_at, id
                """, statement -> { }, loader::rental, "Could not list unassigned rentals");
    }

    @Override
    public boolean claimIfUnassigned(long rentalId, long salesmanId) {
        return execute("Could not atomically claim rental", () -> JdbcSupport.update(connection, """
                UPDATE rentals
                SET salesman_id = ?, status = 'ASSIGNED', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND salesman_id IS NULL AND status = 'REQUESTED'
                  AND EXISTS (
                    SELECT 1 FROM users WHERE id = ? AND role = 'SALESMAN' AND active = TRUE
                  )
                """, statement -> {
            statement.setLong(1, salesmanId);
            statement.setLong(2, rentalId);
            statement.setLong(3, salesmanId);
        }) == 1);
    }

    private static void bindRental(
            java.sql.PreparedStatement statement, Rental rental, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, rental.customer().requireId());
        JdbcSupport.setNullableLong(statement, 2,
                rental.salesman() == null ? null : rental.salesman().requireId());
        statement.setLong(3, rental.vehicle().requireId());
        statement.setObject(4, rental.startsOn());
        statement.setObject(5, rental.endsOn());
        statement.setBigDecimal(6, rental.totalPrice());
        statement.setString(7, rental.status().name());
        if (includeId) {
            statement.setLong(8, rental.requireId());
        }
    }
}
