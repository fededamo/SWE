package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.DiscountDao;
import it.unifi.ing.drivehub.domain.sales.Discount;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

final class PostgresDiscountDao extends AbstractPostgresDao implements DiscountDao {

    PostgresDiscountDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public Discount save(Discount discount) {
        if (discount.id() != null) {
            throw new IllegalArgumentException("discount is already persistent");
        }
        long id = execute("Could not save discount", () -> JdbcSupport.insert(connection, """
                INSERT INTO discounts(name, vehicle_id, percentage, starts_on, ends_on, enabled)
                VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> bindDiscount(statement, discount, false)));
        discount.assignId(id);
        return discount;
    }

    @Override
    public void update(Discount discount) {
        execute("Could not update discount", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE discounts
                SET name = ?, vehicle_id = ?, percentage = ?, starts_on = ?, ends_on = ?, enabled = ?
                WHERE id = ?
                """, statement -> bindDiscount(statement, discount, true)));
    }

    @Override
    public Optional<Discount> findById(long id) {
        return loader.discount(id);
    }

    @Override
    public List<Discount> findAll() {
        return findMany("SELECT id FROM discounts ORDER BY starts_on DESC, id", statement -> { },
                loader::discount, "Could not list discounts");
    }

    @Override
    public List<Discount> findActiveOn(LocalDate date) {
        return findMany("""
                SELECT id FROM discounts
                WHERE enabled = TRUE AND starts_on <= ? AND ends_on >= ?
                ORDER BY percentage DESC, id
                """, statement -> {
            statement.setObject(1, date);
            statement.setObject(2, date);
        }, loader::discount, "Could not list active discounts");
    }

    private static void bindDiscount(
            java.sql.PreparedStatement statement, Discount discount, boolean includeId)
            throws java.sql.SQLException {
        statement.setString(1, discount.name());
        statement.setLong(2, discount.vehicle().requireId());
        statement.setBigDecimal(3, discount.percentage());
        statement.setObject(4, discount.startsOn());
        statement.setObject(5, discount.endsOn());
        statement.setBoolean(6, discount.enabled());
        if (includeId) {
            statement.setLong(7, discount.requireId());
        }
    }
}
