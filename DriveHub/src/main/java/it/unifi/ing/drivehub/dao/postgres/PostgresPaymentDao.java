package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.PaymentDao;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

final class PostgresPaymentDao extends AbstractPostgresDao implements PaymentDao {

    PostgresPaymentDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.id() != null) {
            throw new IllegalArgumentException("payment is already persistent");
        }
        long id = execute("Could not save payment", () -> JdbcSupport.insert(connection, """
                INSERT INTO payments(
                    payer_id, rental_id, sale_order_id, purpose, method, amount, created_at,
                    status, processor_reference, failure_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindPayment(statement, payment, false)));
        payment.assignId(id);
        return payment;
    }

    @Override
    public void update(Payment payment) {
        execute("Could not update payment", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE payments
                SET payer_id = ?, rental_id = ?, sale_order_id = ?, purpose = ?, method = ?, amount = ?,
                    created_at = ?, status = ?, processor_reference = ?, failure_reason = ?
                WHERE id = ?
                """, statement -> bindPayment(statement, payment, true)));
    }

    @Override
    public Optional<Payment> findById(long id) {
        return loader.payment(id);
    }

    @Override
    public List<Payment> findAll() {
        return findMany("SELECT id FROM payments ORDER BY created_at DESC, id", statement -> { },
                loader::payment, "Could not list payments");
    }

    @Override
    public List<Payment> findByReference(PaymentReferenceType referenceType, long referenceId) {
        String column = referenceType == PaymentReferenceType.RENTAL ? "rental_id" : "sale_order_id";
        return findMany("SELECT id FROM payments WHERE " + column + " = ? ORDER BY created_at, id",
                statement -> statement.setLong(1, referenceId), loader::payment,
                "Could not list payments by reference");
    }

    private static void bindPayment(
            java.sql.PreparedStatement statement, Payment payment, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, payment.payer().requireId());
        if (payment.referenceType() == PaymentReferenceType.RENTAL) {
            statement.setLong(2, payment.referenceId());
            statement.setNull(3, Types.BIGINT);
        } else {
            statement.setNull(2, Types.BIGINT);
            statement.setLong(3, payment.referenceId());
        }
        statement.setString(4, payment.purpose().name());
        statement.setString(5, payment.method().name());
        statement.setBigDecimal(6, payment.amount());
        statement.setTimestamp(7, Timestamp.from(payment.createdAt()));
        statement.setString(8, payment.status().name());
        statement.setString(9, payment.processorReference());
        statement.setString(10, payment.failureReason());
        if (includeId) {
            statement.setLong(11, payment.requireId());
        }
    }
}
