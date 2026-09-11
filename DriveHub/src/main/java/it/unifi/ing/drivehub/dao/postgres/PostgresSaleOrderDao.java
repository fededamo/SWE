package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.SaleOrderDao;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class PostgresSaleOrderDao extends AbstractPostgresDao implements SaleOrderDao {

    PostgresSaleOrderDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public SaleOrder save(SaleOrder order) {
        if (order.id() != null) {
            throw new IllegalArgumentException("sale order is already persistent");
        }
        long id = execute("Could not save sale order", () -> JdbcSupport.insert(connection, """
                INSERT INTO sale_orders(
                    customer_id, salesman_id, vehicle_id, total_price, required_deposit, paid_amount, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindOrder(statement, order, false)));
        order.assignId(id);
        return order;
    }

    @Override
    public void update(SaleOrder order) {
        execute("Could not update sale order", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE sale_orders
                SET customer_id = ?, salesman_id = ?, vehicle_id = ?, total_price = ?,
                    required_deposit = ?, paid_amount = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, statement -> bindOrder(statement, order, true)));
    }

    @Override
    public Optional<SaleOrder> findById(long id) {
        return loader.saleOrder(id);
    }

    @Override
    public Optional<SaleOrder> findByIdForUpdate(long id) {
        return findOne("SELECT id FROM sale_orders WHERE id = ? FOR UPDATE",
                statement -> statement.setLong(1, id), loader::saleOrder,
                "Could not lock sale_orders row");
    }

    @Override
    public List<SaleOrder> findByCustomer(long customerId) {
        return findMany("SELECT id FROM sale_orders WHERE customer_id = ? ORDER BY created_at DESC, id",
                statement -> statement.setLong(1, customerId), loader::saleOrder,
                "Could not list sale orders by customer");
    }

    @Override
    public List<SaleOrder> findByStatus(SaleOrderStatus status) {
        return findMany("SELECT id FROM sale_orders WHERE status = ? ORDER BY created_at, id", statement ->
                statement.setString(1, status.name()), loader::saleOrder,
                "Could not list sale orders by status");
    }

    @Override
    public boolean claimIfUnassigned(long orderId, long salesmanId) {
        return execute("Could not atomically claim sale order", () -> JdbcSupport.update(connection, """
                UPDATE sale_orders SET salesman_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND salesman_id IS NULL AND status NOT IN ('COMPLETED', 'CANCELLED')
                  AND EXISTS (
                    SELECT 1 FROM users WHERE id = ? AND role = 'SALESMAN' AND active = TRUE
                  )
                """, statement -> {
            statement.setLong(1, salesmanId);
            statement.setLong(2, orderId);
            statement.setLong(3, salesmanId);
        }) == 1);
    }

    private static void bindOrder(
            java.sql.PreparedStatement statement, SaleOrder order, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, order.customer().requireId());
        JdbcSupport.setNullableLong(statement, 2,
                order.salesman() == null ? null : order.salesman().requireId());
        statement.setLong(3, order.vehicle().requireId());
        statement.setBigDecimal(4, order.totalPrice());
        statement.setBigDecimal(5, order.requiredDeposit());
        statement.setBigDecimal(6, order.paidAmount());
        statement.setString(7, order.status().name());
        if (includeId) {
            statement.setLong(8, order.requireId());
        }
    }
}
