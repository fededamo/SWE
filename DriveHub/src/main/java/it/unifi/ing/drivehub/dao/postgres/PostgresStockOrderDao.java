package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.StockOrderDao;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.StockOrderStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class PostgresStockOrderDao extends AbstractPostgresDao implements StockOrderDao {

    PostgresStockOrderDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public StockOrder save(StockOrder order) {
        if (order.id() != null) {
            throw new IllegalArgumentException("stock order is already persistent");
        }
        long id = execute("Could not save stock order", () -> JdbcSupport.insert(connection, """
                INSERT INTO stock_orders(
                    manager_id, model_id, quantity, unit_cost, placed_on, status, received_on
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindOrder(statement, order, false)));
        order.assignId(id);
        return order;
    }

    @Override
    public void update(StockOrder order) {
        execute("Could not update stock order", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE stock_orders
                SET manager_id = ?, model_id = ?, quantity = ?, unit_cost = ?, placed_on = ?,
                    status = ?, received_on = ?
                WHERE id = ?
                """, statement -> bindOrder(statement, order, true)));
    }

    @Override
    public Optional<StockOrder> findById(long id) {
        return loader.stockOrder(id);
    }

    @Override
    public Optional<StockOrder> findByIdForUpdate(long id) {
        return findOne("SELECT id FROM stock_orders WHERE id = ? FOR UPDATE",
                statement -> statement.setLong(1, id), loader::stockOrder, "Could not lock stock order");
    }

    @Override
    public List<StockOrder> findAll() {
        return findMany("SELECT id FROM stock_orders ORDER BY placed_on DESC, id", statement -> { },
                loader::stockOrder, "Could not list stock orders");
    }

    @Override
    public List<StockOrder> findByStatus(StockOrderStatus status) {
        return findMany("SELECT id FROM stock_orders WHERE status = ? ORDER BY placed_on, id", statement ->
                statement.setString(1, status.name()), loader::stockOrder,
                "Could not list stock orders by status");
    }

    private static void bindOrder(
            java.sql.PreparedStatement statement, StockOrder order, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, order.manager().requireId());
        statement.setLong(2, order.model().requireId());
        statement.setInt(3, order.quantity());
        statement.setBigDecimal(4, order.unitCost());
        statement.setObject(5, order.placedOn());
        statement.setString(6, order.status().name());
        statement.setObject(7, order.receivedOn());
        if (includeId) {
            statement.setLong(8, order.requireId());
        }
    }
}
