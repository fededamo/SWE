package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.rentals.TestDriveStatus;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposalStatus;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.StockOrderStatus;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Optional;

final class JdbcEntityLoader {

    private final Connection connection;

    JdbcEntityLoader(Connection connection) {
        this.connection = connection;
    }

    Optional<User> user(long id) {
        return load("SELECT * FROM users WHERE id = ?", id, row -> new User(
                row.getLong("id"),
                row.getString("fiscal_code"),
                row.getString("first_name"),
                row.getString("last_name"),
                row.getString("email"),
                row.getString("phone"),
                row.getString("password_hash"),
                Role.valueOf(row.getString("role")),
                JdbcSupport.nullableLong(row, "manager_id"),
                row.getBoolean("active")));
    }

    Optional<Brand> brand(long id) {
        return load("SELECT * FROM brands WHERE id = ?", id,
                row -> new Brand(row.getLong("id"), row.getString("name")));
    }

    Optional<VehicleModel> vehicleModel(long id) {
        return load("SELECT * FROM vehicle_models WHERE id = ?", id, row -> new VehicleModel(
                row.getLong("id"),
                requireBrand(row.getLong("brand_id")),
                row.getString("code"),
                row.getString("name"),
                row.getInt("model_year")));
    }

    Optional<Vehicle> vehicle(long id) {
        return load("SELECT * FROM vehicles WHERE id = ?", id, row -> new Vehicle(
                row.getLong("id"),
                row.getString("plate"),
                requireVehicleModel(row.getLong("model_id")),
                VehiclePurpose.valueOf(row.getString("purpose")),
                row.getBigDecimal("sale_price"),
                row.getBigDecimal("daily_rental_rate"),
                row.getLong("mileage"),
                VehicleStatus.valueOf(row.getString("status"))));
    }

    Optional<Discount> discount(long id) {
        return load("SELECT * FROM discounts WHERE id = ?", id, row -> new Discount(
                row.getLong("id"),
                row.getString("name"),
                requireVehicle(row.getLong("vehicle_id")),
                row.getBigDecimal("percentage"),
                row.getObject("starts_on", java.time.LocalDate.class),
                row.getObject("ends_on", java.time.LocalDate.class),
                row.getBoolean("enabled")));
    }

    Optional<Rental> rental(long id) {
        return load("SELECT * FROM rentals WHERE id = ?", id, row -> {
            Long salesmanId = JdbcSupport.nullableLong(row, "salesman_id");
            return new Rental(
                    row.getLong("id"),
                    requireUser(row.getLong("customer_id")),
                    salesmanId == null ? null : requireUser(salesmanId),
                    requireVehicle(row.getLong("vehicle_id")),
                    row.getObject("starts_on", java.time.LocalDate.class),
                    row.getObject("ends_on", java.time.LocalDate.class),
                    row.getBigDecimal("total_price"),
                    RentalStatus.valueOf(row.getString("status")));
        });
    }

    Optional<TestDrive> testDrive(long id) {
        return load("SELECT * FROM test_drives WHERE id = ?", id, row -> {
            Long salesmanId = JdbcSupport.nullableLong(row, "salesman_id");
            var scheduledAt = row.getObject("scheduled_at", java.time.LocalDateTime.class);
            var endsAt = row.getObject("ends_at", java.time.LocalDateTime.class);
            return new TestDrive(
                    row.getLong("id"),
                    requireUser(row.getLong("customer_id")),
                    salesmanId == null ? null : requireUser(salesmanId),
                    requireVehicle(row.getLong("vehicle_id")),
                    scheduledAt,
                    Duration.between(scheduledAt, endsAt),
                    TestDriveStatus.valueOf(row.getString("status")));
        });
    }

    Optional<SaleOrder> saleOrder(long id) {
        return load("SELECT * FROM sale_orders WHERE id = ?", id, row -> {
            Long salesmanId = JdbcSupport.nullableLong(row, "salesman_id");
            return new SaleOrder(
                    row.getLong("id"),
                    requireUser(row.getLong("customer_id")),
                    salesmanId == null ? null : requireUser(salesmanId),
                    requireVehicle(row.getLong("vehicle_id")),
                    row.getBigDecimal("total_price"),
                    row.getBigDecimal("required_deposit"),
                    row.getBigDecimal("paid_amount"),
                    SaleOrderStatus.valueOf(row.getString("status")));
        });
    }

    Optional<Payment> payment(long id) {
        return load("SELECT * FROM payments WHERE id = ?", id, row -> {
            Long rentalId = JdbcSupport.nullableLong(row, "rental_id");
            PaymentReferenceType type = rentalId == null
                    ? PaymentReferenceType.SALE_ORDER : PaymentReferenceType.RENTAL;
            long referenceId = rentalId == null ? row.getLong("sale_order_id") : rentalId;
            Timestamp createdAt = row.getTimestamp("created_at");
            return new Payment(
                    row.getLong("id"),
                    requireUser(row.getLong("payer_id")),
                    type,
                    referenceId,
                    PaymentPurpose.valueOf(row.getString("purpose")),
                    PaymentMethod.valueOf(row.getString("method")),
                    row.getBigDecimal("amount"),
                    createdAt.toInstant(),
                    PaymentStatus.valueOf(row.getString("status")),
                    row.getString("processor_reference"),
                    row.getString("failure_reason"));
        });
    }

    Optional<PurchaseProposal> purchaseProposal(long id) {
        return load("SELECT * FROM purchase_proposals WHERE id = ?", id, row -> {
            Long salesmanId = JdbcSupport.nullableLong(row, "salesman_id");
            Long managerId = JdbcSupport.nullableLong(row, "manager_id");
            return new PurchaseProposal(
                    row.getLong("id"),
                    requireUser(row.getLong("customer_id")),
                    requireVehicle(row.getLong("vehicle_id")),
                    row.getBigDecimal("requested_amount"),
                    row.getString("customer_notes"),
                    row.getTimestamp("requested_at").toInstant(),
                    salesmanId == null ? null : requireUser(salesmanId),
                    row.getBigDecimal("offered_amount"),
                    row.getString("offer_terms"),
                    managerId == null ? null : requireUser(managerId),
                    row.getString("decision_reason"),
                    PurchaseProposalStatus.valueOf(row.getString("status")));
        });
    }

    Optional<StockOrder> stockOrder(long id) {
        return load("SELECT * FROM stock_orders WHERE id = ?", id, row -> new StockOrder(
                row.getLong("id"),
                requireUser(row.getLong("manager_id")),
                requireVehicleModel(row.getLong("model_id")),
                row.getInt("quantity"),
                row.getBigDecimal("unit_cost"),
                row.getObject("placed_on", java.time.LocalDate.class),
                StockOrderStatus.valueOf(row.getString("status")),
                row.getObject("received_on", java.time.LocalDate.class)));
    }

    private User requireUser(long id) throws SQLException {
        return user(id).orElseThrow(() -> missing("user", id));
    }

    private Brand requireBrand(long id) throws SQLException {
        return brand(id).orElseThrow(() -> missing("brand", id));
    }

    private VehicleModel requireVehicleModel(long id) throws SQLException {
        return vehicleModel(id).orElseThrow(() -> missing("vehicle model", id));
    }

    private Vehicle requireVehicle(long id) throws SQLException {
        return vehicle(id).orElseThrow(() -> missing("vehicle", id));
    }

    private SQLException missing(String entity, long id) {
        return new SQLException("Missing " + entity + " referenced by id " + id);
    }

    private <T> Optional<T> load(String sql, long id, RowMapper<T> mapper) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapper.map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load persistent entity", exception);
        }
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet row) throws SQLException;
    }
}
