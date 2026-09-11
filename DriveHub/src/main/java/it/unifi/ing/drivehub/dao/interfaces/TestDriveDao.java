package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.rentals.TestDrive;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TestDriveDao {
    TestDrive save(TestDrive testDrive);
    void update(TestDrive testDrive);
    Optional<TestDrive> findById(long id);

    /** Locks the row until commit/rollback, then reads its current persisted state. */
    Optional<TestDrive> findByIdForUpdate(long id);
    List<TestDrive> findByCustomer(long customerId);
    List<TestDrive> findBySalesman(long salesmanId);
    List<TestDrive> findUnassigned();
    boolean existsOverlapping(long vehicleId, LocalDateTime startsAt, LocalDateTime endsAt);

    /** Atomically confirms a REQUESTED/unassigned booking for one salesman. */
    boolean confirmIfUnassigned(long testDriveId, long salesmanId);
}
