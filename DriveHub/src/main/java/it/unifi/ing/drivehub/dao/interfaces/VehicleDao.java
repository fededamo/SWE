package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.util.List;
import java.util.Optional;

public interface VehicleDao {
    Vehicle save(Vehicle vehicle);
    void update(Vehicle vehicle);
    Optional<Vehicle> findById(long id);

    /** Locks the row until commit/rollback, then reads its current persisted state. */
    Optional<Vehicle> findByIdForUpdate(long id);
    /** Pending or ongoing rentals/test drives for operations that remove availability. */
    boolean hasOpenBookings(long vehicleId);
    Optional<Vehicle> findByPlate(String normalizedPlate);
    List<Vehicle> findAll();
    List<Vehicle> findByModel(long modelId);
    List<Vehicle> findAvailableForSale();
    List<Vehicle> findAvailableForRental();

    /** Compare-and-set used to prevent two bookings from reserving the same vehicle. */
    boolean updateStatusIfCurrent(long vehicleId, VehicleStatus expected, VehicleStatus next);
}
