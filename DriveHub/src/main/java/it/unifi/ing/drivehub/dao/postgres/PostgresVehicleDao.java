package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.VehicleDao;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

final class PostgresVehicleDao extends AbstractPostgresDao implements VehicleDao {

    PostgresVehicleDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (vehicle.id() != null) {
            throw new IllegalArgumentException("vehicle is already persistent");
        }
        long id = execute("Could not save vehicle", () -> JdbcSupport.insert(connection, """
                INSERT INTO vehicles(
                    plate, model_id, purpose, sale_price, daily_rental_rate, mileage, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindVehicle(statement, vehicle, false)));
        vehicle.assignId(id);
        return vehicle;
    }

    @Override
    public void update(Vehicle vehicle) {
        execute("Could not update vehicle", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE vehicles
                SET plate = ?, model_id = ?, purpose = ?, sale_price = ?, daily_rental_rate = ?,
                    mileage = ?, status = ?
                WHERE id = ?
                """, statement -> bindVehicle(statement, vehicle, true)));
    }

    @Override
    public Optional<Vehicle> findById(long id) {
        return loader.vehicle(id);
    }

    @Override
    public Optional<Vehicle> findByPlate(String normalizedPlate) {
        return findOne("SELECT id FROM vehicles WHERE plate = ?", statement ->
                statement.setString(1, Vehicle.normalizePlate(normalizedPlate)), loader::vehicle,
                "Could not find vehicle by plate");
    }

    @Override
    public List<Vehicle> findAll() {
        return findMany("SELECT id FROM vehicles ORDER BY plate, id", statement -> { }, loader::vehicle,
                "Could not list vehicles");
    }

    @Override
    public List<Vehicle> findByModel(long modelId) {
        return findMany("SELECT id FROM vehicles WHERE model_id = ? ORDER BY plate, id", statement ->
                statement.setLong(1, modelId), loader::vehicle, "Could not list vehicles by model");
    }

    @Override
    public List<Vehicle> findAvailableForSale() {
        return findMany("""
                SELECT id FROM vehicles
                WHERE purpose = 'FOR_SALE' AND status = 'AVAILABLE'
                ORDER BY plate, id
                """, statement -> { }, loader::vehicle, "Could not list vehicles available for sale");
    }

    @Override
    public List<Vehicle> findAvailableForRental() {
        return findMany("""
                SELECT id FROM vehicles
                WHERE purpose = 'RENTAL' AND status = 'AVAILABLE'
                ORDER BY plate, id
                """, statement -> { }, loader::vehicle, "Could not list vehicles available for rental");
    }

    @Override
    public boolean updateStatusIfCurrent(long vehicleId, VehicleStatus expected, VehicleStatus next) {
        return execute("Could not atomically transition vehicle status", () -> JdbcSupport.update(connection, """
                UPDATE vehicles SET status = ? WHERE id = ? AND status = ?
                """, statement -> {
            statement.setString(1, next.name());
            statement.setLong(2, vehicleId);
            statement.setString(3, expected.name());
        }) == 1);
    }

    private static void bindVehicle(java.sql.PreparedStatement statement, Vehicle vehicle, boolean includeId)
            throws java.sql.SQLException {
        statement.setString(1, vehicle.plate());
        statement.setLong(2, vehicle.model().requireId());
        statement.setString(3, vehicle.purpose().name());
        if (vehicle.salePrice() == null) {
            statement.setNull(4, Types.NUMERIC);
        } else {
            statement.setBigDecimal(4, vehicle.salePrice());
        }
        if (vehicle.dailyRentalRate() == null) {
            statement.setNull(5, Types.NUMERIC);
        } else {
            statement.setBigDecimal(5, vehicle.dailyRentalRate());
        }
        statement.setLong(6, vehicle.mileage());
        statement.setString(7, vehicle.status().name());
        if (includeId) {
            statement.setLong(8, vehicle.requireId());
        }
    }
}
